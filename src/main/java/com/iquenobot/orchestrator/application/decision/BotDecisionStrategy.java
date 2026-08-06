package com.iquenobot.orchestrator.application.decision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iquenobot.ai.domain.dto.AIMessageDto;
import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.application.ConversationHandoffService;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.BotConfiguration;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionStrategy;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotDecisionStrategy implements DecisionStrategy {

        private final ChatbotService chatbotService;
        private final ConversationRepository conversationRepository;
        private final ConversationMessageRepository messageRepository;
        private final KnowledgeBaseService knowledgeBaseService;
        private final ConversationHandoffService handoffService;
        private final ContactRepository contactRepository;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private record LeadRule(String title, int baseScore) {
        }

        /**
         * Cantidad de mensajes recientes de la conversación que se pasan como contexto
         * al LLM.
         */
        private static final int HISTORY_LIMIT = 30;

        private static final Map<String, LeadRule> COMMERCIAL_RULES = Map.of(
                        "solicitar_precio", new LeadRule("Solicitud de cotización", 40),
                        "consulta_envio", new LeadRule("Consulta de envío", 30),
                        "forma_pago", new LeadRule("Interés en compra - forma de pago", 35),
                        "informacion_empresa", new LeadRule("Interés general en productos/servicios", 25));

        /**
         * Número máximo de intentos de aclaración antes de pasar la conversación al
         * agente.
         */
        private static final int MAX_CLARIFICATION_ATTEMPTS = 1;

        @Override
        public int getPriority() {
                return 10;
        }

        @Override
        public boolean canHandle(ProcessingContext context) {
                if (context.getIncomingMessage().isOutbound()) {
                        return false;
                }

                if (!context.isScheduledProcessing() && context.getConversation() != null
                                && context.getConversation().isPendingAiResponse()) {
                        log.debug("Conversation {} is pending AI response; scheduler will handle it",
                                        context.getConversation().getId());
                        return false;
                }

                BotConfiguration botConfig = context.getBotConfiguration();
                if (botConfig == null || !botConfig.isAiAvailable()) {
                        log.debug("Bot not available (enabled={} provider={})",
                                        botConfig != null ? botConfig.isEnabled() : "N/A",
                                        botConfig != null ? botConfig.getAiProvider() : "N/A");
                        return false;
                }

                var conversation = context.getConversation();

                // -----------------------------------------------------------------
                // RESPETAR TIEMPO DE ESPERA DEL AGENTE
                // -----------------------------------------------------------------
                if (conversation.getAssignedUser() != null
                                && conversation.getStatus() == ConversationStatus.IN_PROGRESS
                                && !handoffService.canBotRespond(conversation)) {

                        log.info("Conversation {} assigned to agent {}. Bot blocked by inactivity window.",
                                        conversation.getId(),
                                        conversation.getAssignedUser().getId());

                        return false;
                }

                // Handoff humano
                boolean wasHandedOff = conversation.isHumanHandoff();
                if (!handoffService.canBotRespond(conversation)) {
                        log.debug("Bot paused by human handoff for conversation {}", conversation.getId());
                        return false;
                }

                if (wasHandedOff) {
                        conversationRepository.save(conversation);
                }

                return true;
        }

        @Override
        public Decision decide(ProcessingContext context) {
                var msg = context.getIncomingMessage();
                var botConfig = context.getBotConfiguration();

                try {
                        var conversation = context.getConversation();
                        boolean isFirstMessage = conversation.getMessageCount() == 0;

                        // Para mensajes multimedia sin texto (audio, sticker, foto sin caption),
                        // se pasa el tipo como contexto al chatbot para que no reciba texto vacío.
                        String content = msg.getContent() != null && !msg.getContent().isBlank()
                                        ? msg.getContent()
                                        : "[" + msg.getType() + "]";

                        ChatbotResponseDto botResponse = chatbotService.processMessage(
                                        content,
                                        Map.of(
                                                        "conversationId", conversation.getId().toString(),
                                                        "tenantId", context.getTenantId().toString(),
                                                        "contactId", context.getContact().getId().toString(),
                                                        "channel", msg.getChannel().name(),
                                                        "systemPrompt",
                                                        botConfig != null ? botConfig.getSystemPrompt() : "",
                                                        "fallbackMessage",
                                                        botConfig != null ? botConfig.getFallbackMessage() : "",
                                                        "temperature",
                                                        String.valueOf(botConfig != null ? botConfig.getTemperature()
                                                                        : 0.7),
                                                        "isFirstMessage", isFirstMessage,
                                                        "history", buildConversationHistory(context, msg)));

                        if (botResponse.isRequiresHumanAgent()) {
                                resetFallbackCount(context);
                                return Decision.builder()
                                                .actionType(ActionType.TRANSFER_CONVERSATION)
                                                .reason("Customer requested human agent or bot error")
                                                .requiresAgent(true)
                                                .parameters(Map.of(
                                                                "response", botResponse.getMessage(),
                                                                "intent", botResponse.getIntentDetected() != null
                                                                                ? botResponse.getIntentDetected()
                                                                                : "unknown"))
                                                .build();
                        }

                        if (botResponse.isRequiresClarification()) {
                                return handleClarification(context, botResponse);
                        }

                        resetFallbackCount(context);

                        String intent = botResponse.getIntentDetected() != null
                                        ? botResponse.getIntentDetected()
                                        : "unknown";

                        boolean purchaseIntent = hasPurchaseIntent(context, intent);

                        LeadRule rule = COMMERCIAL_RULES.get(intent);
                        if (rule != null) {
                                context.addSecondaryDecision(Decision.builder()
                                                .actionType(ActionType.CREATE_LEAD)
                                                .reason("Commercial intent detected: " + intent)
                                                .parameters(Map.of(
                                                                "intent", intent,
                                                                "title", rule.title(),
                                                                "baseScore", String.valueOf(rule.baseScore()),
                                                                "messageContent", msg.getContent()))
                                                .build());
                        }

                        if (purchaseIntent) {
                                log.info("Purchase intent detected for conversation {}; transferring to agent for closing",
                                                conversation.getId());
                                return Decision.builder()
                                                .actionType(ActionType.TRANSFER_CONVERSATION)
                                                .reason("Customer shows purchase intent: " + intent)
                                                .requiresAgent(true)
                                                .parameters(Map.of(
                                                                "response", botResponse.getMessage(),
                                                                "intent", intent))
                                                .build();
                        }

                        // -----------------------------------------------------------------
                        // COTIZACIÓN PENDIENTE: el cliente ya pidió una cotización y el bot
                        // le solicitó sus datos. Si la respuesta completa su perfil, se genera
                        // la cotización que quedó en espera; si aún faltan datos, se le vuelve
                        // a pedir con cortesía. Una cotización pendiente caduca pasada la
                        // vigencia (TTL) para no seguir pidiendo datos por una solicitud vieja
                        // o detectada por error.
                        // -----------------------------------------------------------------
                        if (isPendingQuote(conversation)) {
                                if (isPendingQuoteExpired(conversation)) {
                                        clearPendingQuote(conversation);
                                        log.info("Pending quote expired for conversation {}; clearing stale request",
                                                        conversation.getId());
                                } else {
                                        Contact pendingContact = reloadContact(context);
                                        if (hasQuoteEligibilityData(pendingContact)) {
                                                List<UUID> pendingProductIds = pendingQuoteProductIds(conversation);
                                                clearPendingQuote(conversation);
                                                if (!pendingProductIds.isEmpty()) {
                                                        log.info("Pending quote ready for conversation {}: contact {} now has billing data; generating quote",
                                                                        conversation.getId(), pendingContact.getId());
                                                        return Decision.builder()
                                                                        .actionType(ActionType.SEND_QUOTE)
                                                                        .reason("Pending quote completed with customer data")
                                                                        .parameters(Map.of(
                                                                                        "productIds", pendingProductIds.stream()
                                                                                                        .map(UUID::toString)
                                                                                                        .collect(Collectors.joining(",")),
                                                                                        "messageContent",
                                                                                        msg.getContent() != null
                                                                                                        ? msg.getContent()
                                                                                                        : ""))
                                                                        .build();
                                                }
                                        } else {
                                                return Decision.builder()
                                                                .actionType(ActionType.SEND_TEXT)
                                                                .reason("Customer still missing billing data for pending quote")
                                                                .parameters(Map.of(
                                                                                "response", buildDataRequestMessage(pendingContact),
                                                                                "intent", intent))
                                                                .build();
                                        }
                                }
                        }

                        if ("solicitar_precio".equals(intent)) {
                                List<UUID> productIds = findMatchingProductIds(context, msg);
                                if (!productIds.isEmpty()) {
                                        // Guardia: jamás se cotiza a un desconocido. Antes de generar
                                        // el PDF se verifica que el cliente haya compartido sus datos
                                        // (nombre + DNI/RUC o razón social). Si faltan, se le piden con
                                        // cortesía y la cotización queda pendiente hasta recibirlos.
                                        Contact quoteContact = reloadContact(context);
                                        if (!hasQuoteEligibilityData(quoteContact)) {
                                                markPendingQuote(conversation, productIds);
                                                log.info("Quote requested for conversation {} but contact {} lacks billing data; asking for it",
                                                                conversation.getId(), quoteContact.getId());
                                                return Decision.builder()
                                                                .actionType(ActionType.SEND_TEXT)
                                                                .reason("Customer requested a quote; billing data required")
                                                                .parameters(Map.of(
                                                                                "response", buildDataRequestMessage(quoteContact),
                                                                                "intent", intent))
                                                                .build();
                                        }
                                        clearPendingQuote(conversation);
                                        log.info("Quote requested for conversation {} with {} product(s); generating quote PDF",
                                                        conversation.getId(), productIds.size());
                                        return Decision.builder()
                                                        .actionType(ActionType.SEND_QUOTE)
                                                        .reason("Customer requested a quote")
                                                        .parameters(Map.of(
                                                                        "productIds", productIds.stream()
                                                                                        .map(UUID::toString)
                                                                                        .collect(Collectors
                                                                                                        .joining(",")),
                                                                        "messageContent",
                                                                        msg.getContent() != null ? msg.getContent()
                                                                                        : ""))
                                                        .build();
                                }
                        }

                        return Decision.builder()
                                        .actionType(ActionType.SEND_TEXT)
                                        .reason("Bot responded to message")
                                        .parameters(Map.of(
                                                        "response", botResponse.getMessage(),
                                                        "intent", intent,
                                                        "flowExecuted", botResponse.getFlowExecuted() != null
                                                                        ? botResponse.getFlowExecuted()
                                                                        : ""))
                                        .build();

                } catch (Exception e) {
                        log.warn("Chatbot processing failed, transferring to human: {}", e.getMessage());
                        return Decision.builder()
                                        .actionType(ActionType.TRANSFER_CONVERSATION)
                                        .reason("Bot error: " + e.getMessage())
                                        .requiresAgent(true)
                                        .build();
                }
        }

        /**
         * El bot actúa como filtro de calificación: cuando no entiende al cliente,
         * le pide con cortesía que detalle su consulta y mantiene la conversación.
         * Solo transfiere al agente humano si el cliente vuelve a escribir algo
         * que no se entiende (agotó los intentos de aclaración).
         */
        private Decision handleClarification(ProcessingContext context, ChatbotResponseDto botResponse) {
                var conversation = context.getConversation();
                int fallbacks = conversation.getBotFallbackCount();

                if (fallbacks >= MAX_CLARIFICATION_ATTEMPTS) {
                        log.info("Conversation {} unable to understand customer after {} attempts; transferring to agent",
                                        conversation.getId(), fallbacks + 1);
                        resetFallbackCount(context);
                        return Decision.builder()
                                        .actionType(ActionType.TRANSFER_CONVERSATION)
                                        .reason("Unable to understand customer after clarification attempts")
                                        .requiresAgent(true)
                                        .parameters(Map.of(
                                                        "response", botResponse.getMessage(),
                                                        "intent", "unknown"))
                                        .build();
                }

                conversation.setBotFallbackCount(fallbacks + 1);
                conversationRepository.save(conversation);
                log.info("Conversation {} not understood (attempt {}/{}); asking customer to clarify",
                                conversation.getId(), fallbacks + 1, MAX_CLARIFICATION_ATTEMPTS + 1);

                return Decision.builder()
                                .actionType(ActionType.SEND_TEXT)
                                .reason("Bot requested clarification")
                                .parameters(Map.of(
                                                "response", botResponse.getMessage(),
                                                "intent", "unknown"))
                                .build();
        }

        private boolean hasPurchaseIntent(ProcessingContext context, String intent) {
                String content = context.getIncomingMessage().getContent();
                return content != null && ChatbotService.PURCHASE_INTENT_PATTERN.matcher(content.toLowerCase()).find();
        }

        /**
         * Construye el historial reciente de la conversación en formato de mensajes
         * para el LLM (user/assistant). Esto permite al bot interpretar mensajes que
         * dependen del contexto previo, p. ej. "la opción 4" cuando antes listó
         * productos. El mensaje que se está procesando se excluye: ChatbotService lo
         * agrega como última entrada al generar la respuesta.
         */
        private List<AIMessageDto> buildConversationHistory(ProcessingContext context, IncomingMessage msg) {
                var conversation = context.getConversation();
                if (conversation.getId() == null) {
                        return List.of();
                }
                Page<ConversationMessage> page = messageRepository.findByConversationIdOrderBySentAtDesc(
                                conversation.getId(), PageRequest.of(0, HISTORY_LIMIT));
                List<ConversationMessage> recent = new ArrayList<>(page.getContent());
                Collections.reverse(recent);

                String currentChannelMessageId = msg.getChannelMessageId();
                List<AIMessageDto> history = new ArrayList<>();
                for (ConversationMessage m : recent) {
                        // Solo se excluyen los mensajes entrantes aún no consumidos por la
                        // consolidación (ya están contenidos en el texto consolidado actual).
                        // Los mensajes salientes (respuestas del bot o del agente) siempre
                        // forman parte del contexto, aunque su ai_processed sea false.
                        if (m.isInbound() && !m.isAiProcessed()) {
                                continue;
                        }
                        if (currentChannelMessageId != null
                                        && currentChannelMessageId.equals(m.getChannelMessageId())) {
                                continue;
                        }
                        String content = m.getContent();
                        if (content == null || content.isBlank()) {
                                continue;
                        }
                        history.add(AIMessageDto.builder()
                                        .role(m.isInbound() ? "user" : "assistant")
                                        .content(content)
                                        .build());
                }
                return history;
        }

        private List<UUID> findMatchingProductIds(ProcessingContext context, IncomingMessage msg) {
                String content = msg.getContent();
                if (content == null || content.isBlank()) {
                        return List.of();
                }
                return knowledgeBaseService.findMatchingProducts(context.getTenantId(), content)
                                .stream().map(p -> p.getId()).toList();
        }

        /**
         * Clave usada en la metadata de la conversación para recordar que hay una
         * cotización en espera mientras el cliente envía sus datos.
         */
        private static final String PENDING_QUOTE_KEY = "pending_quote_product_ids";
        private static final String PENDING_QUOTE_CREATED_AT_KEY = "pending_quote_created_at";

        /**
         * Vigencia máxima de una cotización pendiente: si el cliente no envía sus
         * datos dentro de este lapso, la solicitud pendiente se descarta para que
         * el bot no siga pidiendo datos de facturación por una cotización vieja
         * o detectada por error.
         */
        private static final Duration PENDING_QUOTE_TTL = Duration.ofHours(1);

        /**
         * Re-carga el contacto desde la BD para reflejar los datos que el cliente
         * pudo haber enviado en el mensaje actual (DNI/RUC, razón social, dirección).
         */
        private Contact reloadContact(ProcessingContext context) {
                if (context.getContact() == null || context.getContact().getId() == null) {
                        return context.getContact();
                }
                return contactRepository.findByIdAndTenantIdAndDeletedFalse(
                                context.getContact().getId(), context.getTenantId())
                                .orElse(context.getContact());
        }

        /**
         * El bot jamás cotiza a un desconocido: exige que el cliente haya compartido
         * su nombre completo (o de su empresa) y un documento de identidad (DNI/RUC)
         * o la razón social. La dirección es deseable pero no bloqueante.
         */
        private boolean hasQuoteEligibilityData(Contact contact) {
                if (contact == null) {
                        return false;
                }
                boolean hasName = hasText(contact.getFullName())
                                || hasText(contact.getFirstName())
                                || hasText(contact.getLastName());
                boolean hasCompany = hasText(contact.getCompany());
                boolean hasTaxDocument = hasText(contact.getDocumentNumber());
                return hasName && (hasCompany || hasTaxDocument);
        }

        /**
         * Mensaje cortés con el que el bot solicita los datos del cliente o empresa
         * antes de elaborar la cotización. Solo lista los datos que faltan.
         */
        private String buildDataRequestMessage(Contact contact) {
                String greeting = contact != null && hasText(contact.getFirstName())
                                ? contact.getFirstName() + ", con mucho gusto"
                                : "con mucho gusto";
                StringBuilder sb = new StringBuilder("Hola, ")
                                .append(greeting)
                                .append(" te prepararé tu cotización. Para enviártela formalmente "
                                                + "necesito que me compartas estos datos, por favor:\n\n");
                List<String> missing = new ArrayList<>();
                boolean hasName = contact != null && (hasText(contact.getFullName())
                                || hasText(contact.getFirstName())
                                || hasText(contact.getLastName()));
                if (!hasName) {
                        missing.add("- Tu nombre completo (o razón social)");
                }
                boolean hasCompany = contact != null && hasText(contact.getCompany());
                boolean hasTaxDocument = contact != null && hasText(contact.getDocumentNumber());
                if (!hasTaxDocument && !hasCompany) {
                        missing.add("- Tu DNI o RUC (o el de tu empresa)");
                }
                if (contact == null || !hasText(contact.getAddress())) {
                        missing.add("- Tu dirección fiscal (opcional)");
                }
                sb.append(String.join("\n", missing));
                sb.append("\n\nCon esos datos tu cotización quedará lista al instante. "
                                + "¡Gracias por tu confianza!");
                return sb.toString();
        }

        private boolean isPendingQuote(Conversation conversation) {
                return !pendingQuoteProductIds(conversation).isEmpty();
        }

        /**
         * Una cotización pendiente caduca pasada la vigencia (TTL): si el cliente
         * no envió sus datos a tiempo, se descarta para que el bot no siga pidiendo
         * datos de facturación por una solicitud vieja o detectada por error.
         * La metadata sin marca de tiempo (datos previos a esta protección) nunca
         * se considera caducada, para no cambiar el comportamiento existente.
         */
        private boolean isPendingQuoteExpired(Conversation conversation) {
                if (conversation == null || conversation.getMetadata() == null
                                || conversation.getMetadata().isBlank()) {
                        return false;
                }
                try {
                        JsonNode node = objectMapper.readTree(conversation.getMetadata());
                        JsonNode createdAt = node.get(PENDING_QUOTE_CREATED_AT_KEY);
                        if (createdAt == null || !createdAt.isNumber()) {
                                return false;
                        }
                        long ageMillis = System.currentTimeMillis() - createdAt.asLong();
                        return ageMillis > PENDING_QUOTE_TTL.toMillis();
                } catch (Exception e) {
                        log.debug("Could not read pending quote age from conversation metadata: {}",
                                        e.getMessage());
                        return false;
                }
        }

        private List<UUID> pendingQuoteProductIds(Conversation conversation) {
                if (conversation == null || conversation.getMetadata() == null
                                || conversation.getMetadata().isBlank()) {
                        return List.of();
                }
                try {
                        JsonNode node = objectMapper.readTree(conversation.getMetadata());
                        JsonNode ids = node.get(PENDING_QUOTE_KEY);
                        if (ids == null || !ids.isArray()) {
                                return List.of();
                        }
                        List<UUID> result = new ArrayList<>();
                        for (JsonNode id : ids) {
                                try {
                                        result.add(UUID.fromString(id.asText()));
                                } catch (IllegalArgumentException ignored) {
                                        // id malformado en metadata; se omite
                                }
                        }
                        return result;
                } catch (Exception e) {
                        log.debug("Could not read pending quote from conversation metadata: {}", e.getMessage());
                        return List.of();
                }
        }

        private void markPendingQuote(Conversation conversation, List<UUID> productIds) {
                try {
                        ObjectNode node;
                        if (conversation.getMetadata() == null || conversation.getMetadata().isBlank()) {
                                node = objectMapper.createObjectNode();
                        } else {
                                node = (ObjectNode) objectMapper.readTree(conversation.getMetadata());
                        }
                        if (node.has(PENDING_QUOTE_KEY)) {
                                node.remove(PENDING_QUOTE_KEY);
                        }
                        node.put(PENDING_QUOTE_CREATED_AT_KEY, System.currentTimeMillis());
                        ArrayNode arr = node.putArray(PENDING_QUOTE_KEY);
                        productIds.forEach(id -> arr.add(id.toString()));
                        conversation.setMetadata(objectMapper.writeValueAsString(node));
                        conversationRepository.save(conversation);
                } catch (Exception e) {
                        log.warn("Could not mark pending quote for conversation {}: {}",
                                        conversation.getId(), e.getMessage());
                }
        }

        private void clearPendingQuote(Conversation conversation) {
                if (conversation == null || conversation.getMetadata() == null
                                || conversation.getMetadata().isBlank()) {
                        return;
                }
                try {
                        JsonNode node = objectMapper.readTree(conversation.getMetadata());
                        if (node instanceof ObjectNode objectNode
                                        && (objectNode.has(PENDING_QUOTE_KEY)
                                                        || objectNode.has(PENDING_QUOTE_CREATED_AT_KEY))) {
                                objectNode.remove(PENDING_QUOTE_KEY);
                                objectNode.remove(PENDING_QUOTE_CREATED_AT_KEY);
                                conversation.setMetadata(objectMapper.writeValueAsString(objectNode));
                                conversationRepository.save(conversation);
                        }
                } catch (Exception e) {
                        log.warn("Could not clear pending quote for conversation {}: {}",
                                        conversation.getId(), e.getMessage());
                }
        }

        private boolean hasText(String value) {
                return value != null && !value.isBlank();
        }

        private void resetFallbackCount(ProcessingContext context) {
                var conversation = context.getConversation();
                if (conversation.getBotFallbackCount() != 0) {
                        conversation.setBotFallbackCount(0);
                        conversationRepository.save(conversation);
                }
        }
}
