package com.iquenobot.chatbot.application;

import com.iquenobot.ai.domain.dto.AIMessageDto;
import com.iquenobot.ai.domain.dto.AIResponseDto;
import com.iquenobot.ai.domain.service.IAIProvider;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.chatbot.domain.entity.ChatbotFlow;
import com.iquenobot.chatbot.domain.entity.ChatbotIntent;
import com.iquenobot.chatbot.domain.repository.ChatbotFlowRepository;
import com.iquenobot.chatbot.domain.repository.ChatbotIntentRepository;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ChatbotFlowTrigger;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatbotFlowRepository flowRepository;
    private final ChatbotIntentRepository intentRepository;
    private final IAIProvider aiProvider;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private static final java.util.regex.Pattern REQUEST_HUMAN_PATTERN = java.util.regex.Pattern.compile(
            "\\b(agente|representante|humano|asesor)\\b|hablar con (alguien|una persona|un agente|un representante)"
                    + "|(quiero|necesito|prefiero) (hablar|que me atienda) (con|un|una)");

    /** Frases que indican intención de compra inequívoca (el cliente confirma querer comprar). */
    public static final java.util.regex.Pattern PURCHASE_INTENT_PATTERN = java.util.regex.Pattern.compile(
            "\\b(comprar|compré|comprarlo|comprarla|comprarlos|adquirir|encargar|hacer (un|el|mi) pedido"
                    + "|orden de compra)\\b"
                    + "|(?<!no )(quiero (comprar|adquirir|encargar|pedir|hacer (un|el|mi) pedido"
                    + "|uno|una|este|esta|ese|esa))"
                    + "|(?<!no )me interesa (comprar|adquirir|hacer (un|el) pedido)");

    /**
     * Señales de una solicitud de cotización en un mensaje que no matcheó ningún
     * intent ni flow: pedido explícito de cotización/presupuesto, cantidad
     * deseada ("deseo 2 unidades") o forma de pago mencionada. Cuando se detectan,
     * la respuesta del LLM se marca con el intent "solicitar_precio" para que el
     * orquestador genere el PDF y lo envíe por WhatsApp.
     */
    public static final java.util.regex.Pattern QUOTE_REQUEST_PATTERN = java.util.regex.Pattern.compile(
            "\\bcotiz\\w*\\b|\\bpresupuest\\w*\\b"
                    + "|(deseo|quiero|necesito|requiero)\\s+\\d+\\s*(unidades?|unds?|unid\\.?|und\\.?)"
                    + "|(?:me\\s+|nos\\s+)?(?:puedes|puede|podrías|podrias|quieres|deseas)?\\s*"
                    + "(?:mandar|enviar|pasar|hacer|elaborar|dar)\\w*\\s+(?:una\\s+|la\\s+|un\\s+)?"
                    + "(?:cotizaci\\w+|presupuest\\w+)"
                    + "|pago\\s+(?:al\\s+|de\\s+)?(?:contado|cash|crédito|credito|letras|plazos"
                    + "|transferencia|yape|plin|efectivo|contra\\s+entrega)");

    /**
     * Términos que indican una consulta comercial genérica ("¿qué venden?",
     * "¿qué ofrecen?", "catálogo", "precios"...). Aunque no exista contexto
     * previo ni match de KB, el bot responde con el LLM en lugar de pedir
     * aclaración, para no rechazar consultas legítimas de venta.
     */
    private static final String[] SALES_INQUIRY_TERMS = {
            "que venden", "q venden", "que vende", "q vende", "que vendes", "q vendes",
            "que ofrecen", "q ofrecen", "que ofrece", "q ofrece", "que ofreces",
            "que tienen", "q tienen", "que manejan", "q manejan", "que productos",
            "catalogo", "catálogo", "sus productos", "tus productos", "los productos",
            "servicios que", "informacion de productos", "información de productos",
            "informacion de sus productos", "información de sus productos",
            "precios", "precio de", "cuanto cuesta", "cuánto cuesta", "cuanto vale", "cuánto vale",
            "que es lo que venden", "q es lo que venden", "a que se dedican", "a qué se dedican",
            "que vende la empresa", "q vende la empresa", "en que trabajan", "en qué trabajan",
            "productos que venden", "productos que ofrecen", "que tipo de productos",
            "q tipo de productos", "que oferta", "q oferta"
    };

    private boolean isSalesInquiry(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        for (String term : SALES_INQUIRY_TERMS) {
            if (normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public ChatbotResponseDto processMessage(String message, Map<String, Object> context) {
        UUID tenantId = getTenantId();
        
        log.info("Processing chatbot message for tenant: {}", tenantId);

        // Si el cliente envía sus datos de facturación (DNI/RUC, razón social,
        // dirección), se capturan y guardan en su contacto antes de responder.
        captureContactDetails(message, context);

        // 1. Try to detect intent
        ChatbotIntent intent = detectIntent(tenantId, message);
        
        // 2. Try to match a flow
        ChatbotFlow flow = matchFlow(tenantId, message, intent, context);
        
        // 3. Generate response
        if (flow != null) {
            ChatbotResponseDto response = executeFlow(flow, message, context);
            // Un flow no debe descartar la intención comercial ya detectada: se propaga
            // para que el orquestador pueda disparar las acciones correspondientes
            // (cotización en PDF, creación de lead, etc.).
            if (intent != null && response.getIntentDetected() == null) {
                response.setIntentDetected(intent.getIntentName());
            }
            return response;
        } else if (intent != null) {
            return executeIntent(intent, message);
        } else {
            return handleFallback(message, context);
        }
    }

    /**
     * Respuesta de respaldo cuando no hay intent ni flow configurado.
     *
     * Si existe conocimiento relevante en la KB, en el catálogo de productos o
     * historial previo de la conversación, el bot responde con el LLM usando ese
     * contexto (esto permite interpretar referencias como "la opción 4" o
     * acuses como "Interesante" tras una respuesta del bot). De lo contrario NO
     * responde con el LLM (evita respuestas inventadas a mensajes fuera de
     * alcance o personales), sino que pide al cliente que detalle su consulta.
     * La transferencia al agente humano solo se produce si el cliente la solicita
     * explícitamente o si el orquestador agota los intentos de aclaración.
     */
    private ChatbotResponseDto handleFallback(String message, Map<String, Object> context) {
        boolean requestsHuman = REQUEST_HUMAN_PATTERN.matcher(message.toLowerCase()).find();
        if (requestsHuman) {
            log.info("Customer explicitly requested a human agent; transferring");
            return ChatbotResponseDto.builder()
                    .message(resolveFallbackMessage(context))
                    .requiresHumanAgent(true)
                    .requiresClarification(false)
                    .suggestedActions(new ArrayList<>())
                    .entities(new HashMap<>())
                    .build();
        }

        @SuppressWarnings("unchecked")
        List<AIMessageDto> history = (List<AIMessageDto>) context.getOrDefault("history", new ArrayList<>());
        boolean hasConversationContext = history.stream().anyMatch(m -> "assistant".equals(m.getRole()));

        // Las consultas comerciales genéricas ("¿qué venden?", "catálogo", "precios"...)
        // SIEMPRE se responden con el LLM (con el contexto de KB/productos del tenant
        // si existe), para no rechazar consultas legítimas de venta con "no entendí".
        String kbContext = knowledgeBaseService.buildContextForQuery(buildContextQuery(message, history));
        if (hasConversationContext || !kbContext.isEmpty() || isSalesInquiry(message)) {
            return generateAIResponse(message, context, kbContext);
        }

        log.info("No intent, flow, KB or product context matched; asking customer to clarify");
        return ChatbotResponseDto.builder()
                .message(resolveFallbackMessage(context))
                .requiresHumanAgent(false)
                .requiresClarification(true)
                .suggestedActions(new ArrayList<>())
                .entities(new HashMap<>())
                .build();
    }

    private String resolveFallbackMessage(Map<String, Object> context) {
        return context.get("fallbackMessage") != null
                ? (String) context.get("fallbackMessage")
                : "Gracias por escribirnos. Disculpa, no logré entender completamente tu consulta. "
                        + "Para poder ayudarte de la mejor manera, ¿podrías indicarme con mayor detalle qué es "
                        + "exactamente lo que deseas? Por ejemplo: el nombre del producto, el modelo o la "
                        + "información que necesitas. Un agente humano está al tanto de tu mensaje y te "
                        + "atenderá muy pronto. ¡Gracias por tu paciencia!";
    }

    /**
     * Combina el mensaje actual con los últimos mensajes del bot para que la
     * búsqueda en la base de conocimiento y el catálogo pueda resolver
     * referencias (p. ej. "la opción 4" o "ese modelo") contra lo que el bot
     * mostró antes en la conversación.
     */
    private String buildContextQuery(String message, List<AIMessageDto> history) {
        StringBuilder sb = new StringBuilder(message == null ? "" : message);
        int appended = 0;
        for (int i = history.size() - 1; i >= 0 && appended < 2; i--) {
            AIMessageDto m = history.get(i);
            if ("assistant".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                sb.append(' ').append(m.getContent());
                appended++;
            }
        }
        return sb.length() > 1000 ? sb.substring(0, 1000) : sb.toString();
    }

    @Transactional(readOnly = true)
    public ChatbotIntent detectIntent(UUID tenantId, String message) {
        List<ChatbotIntent> activeIntents = intentRepository.findByTenantIdAndActiveAndDeletedFalseOrderByPriorityDesc(
                tenantId, true);
        
        for (ChatbotIntent intent : activeIntents) {
            // Simple keyword matching (in production, use NLP/ML)
            String[] keywords = extractKeywords(intent.getTrainingPhrases());
            for (String keyword : keywords) {
                if (message.toLowerCase().contains(keyword.toLowerCase())) {
                    intent.incrementMatchedCount();
                    intentRepository.save(intent);
                    return intent;
                }
            }
        }
        
        return null;
    }

    @Transactional(readOnly = true)
    public ChatbotFlow matchFlow(UUID tenantId, String message, ChatbotIntent intent, Map<String, Object> context) {
        // 1. Try keyword trigger
        List<ChatbotFlow> flows = flowRepository.findByKeyword(tenantId, message);
        if (!flows.isEmpty()) {
            return flows.get(0);
        }

        // 2. Try WELCOME trigger (first message in conversation)
        if (context != null && Boolean.TRUE.equals(context.get("isFirstMessage"))) {
            flows = flowRepository.findByTenantIdAndTriggerTypeAndActiveAndDeletedFalse(
                    tenantId, ChatbotFlowTrigger.WELCOME, true);
            if (!flows.isEmpty()) {
                return flows.get(0);
            }
        }

        // 3. Try intent trigger
        if (intent != null) {
            flows = flowRepository.findByTenantIdAndTriggerTypeAndActiveAndDeletedFalse(
                    tenantId, ChatbotFlowTrigger.INTENT, true);
            if (!flows.isEmpty()) {
                return flows.get(0);
            }
        }

        return null;
    }

    @Transactional
    public ChatbotResponseDto executeFlow(ChatbotFlow flow, String message, Map<String, Object> context) {
        log.info("Executing flow: {}", flow.getName());
        
        try {
            flow.incrementSuccess();
            flowRepository.save(flow);
            
            String botMessage = extractFlowMessage(flow.getFlowConfig());
            if (botMessage == null) {
                botMessage = flow.getFallbackMessage() != null ? flow.getFallbackMessage() : "Procesando tu solicitud...";
            }
            
            return ChatbotResponseDto.builder()
                    .message(botMessage)
                    .flowExecuted(flow.getName())
                    .requiresHumanAgent(false)
                    .suggestedActions(new ArrayList<>())
                    .entities(new HashMap<>())
                    .build();
        } catch (Exception e) {
            flow.incrementFailure();
            flowRepository.save(flow);
            throw e;
        }
    }

    private String extractFlowMessage(String flowConfig) {
        if (flowConfig == null || flowConfig.isBlank()) return null;
        try {
            var node = objectMapper.readTree(flowConfig);
            var msg = node.get("message");
            return msg != null ? msg.asText() : null;
        } catch (JsonProcessingException e) {
            log.warn("Invalid flowConfig JSON: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public ChatbotResponseDto executeIntent(ChatbotIntent intent, String message) {
        log.info("Executing intent: {}", intent.getIntentName());
        
        String[] responses = extractResponses(intent.getResponses());
        String selectedResponse = responses.length > 0
                ? responses[random.nextInt(responses.length)]
                : "¿En qué puedo ayudarte?";
        
        return ChatbotResponseDto.builder()
                .message(selectedResponse)
                .intentDetected(intent.getIntentName())
                .confidence(intent.getConfidenceThreshold())
                .requiresHumanAgent(false)
                .suggestedActions(new ArrayList<>())
                .entities(new HashMap<>())
                .build();
    }

    @Transactional
    public ChatbotResponseDto generateAIResponse(String message, Map<String, Object> context) {
        String kbContext = context.get("kbContext") != null
                ? (String) context.get("kbContext")
                : knowledgeBaseService.buildContextForQuery(message);
        return generateAIResponse(message, context, kbContext);
    }

    @Transactional
    public ChatbotResponseDto generateAIResponse(String message, Map<String, Object> context, String kbContext) {
        log.info("Generating AI response");

        try {
            String systemPrompt = (String) context.get("systemPrompt");

            // El bot siempre se alimenta del módulo de conocimientos: los documentos
            // de comportamiento del tenant (reglas, tono, procedimientos) definen cómo
            // debe portarse y qué debe realizar en cada respuesta.
            String behaviorContext = knowledgeBaseService.buildBehaviorContext();
            if (behaviorContext != null && !behaviorContext.isEmpty()) {
                systemPrompt = (systemPrompt != null ? systemPrompt : "") + behaviorContext;
            }

            if (kbContext != null && !kbContext.isEmpty()) {
                systemPrompt = (systemPrompt != null ? systemPrompt : "") + kbContext;
            }

            AIMessageDto aiMessage = AIMessageDto.builder()
                    .role("user")
                    .content(message)
                    .build();
            
            List<AIMessageDto> history = (List<AIMessageDto>) context.getOrDefault("history", new ArrayList<>());
            history.add(aiMessage);
            
            Double temperature = context.get("temperature") instanceof String t ? Double.parseDouble(t) : null;
            AIResponseDto aiResponse = aiProvider.chatCompletion(history, systemPrompt, temperature, null);
            
            ChatbotResponseDto response = ChatbotResponseDto.builder()
                    .message(aiResponse.getContent())
                    .confidence(0.0)
                    .requiresHumanAgent(false)
                    .suggestedActions(new ArrayList<>())
                    .entities(new HashMap<>())
                    .build();

            // Si el mensaje del cliente es una solicitud de cotización que no matcheó
            // ningún intent ni flow (p. ej. "deseo 2 unidades, pago al contado"), se
            // marca la respuesta para que el orquestador genere y envíe el PDF.
            if (message != null && QUOTE_REQUEST_PATTERN.matcher(message.toLowerCase()).find()) {
                response.setIntentDetected("solicitar_precio");
            }
            return response;
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return ChatbotResponseDto.builder()
                    .message("Lo siento, no pude procesar tu mensaje. ¿Puedo conectarte con un agente?")
                    .requiresHumanAgent(true)
                    .build();
        }
    }

    /**
     * Señales de que el mensaje del cliente contiene datos de facturación
     * (DNI/RUC, dirección, razón social). Se usa solo para evitar invocar al
     * extractor en mensajes irrelevantes.
     */
    private static final java.util.regex.Pattern TAX_DETAILS_TRIGGER = java.util.regex.Pattern.compile(
            "\\b(dni|ruc|documento|raz[oó]n\\s+social|direcci[oó]n|av\\.|calle|jr\\.|urb\\.|mz\\.|lt\\.)\\b"
                    + "|\\b\\d{8}\\b|\\b\\d{11}\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Señales de razón social o dirección: solo cuando aparecen se invoca al
     * LLM para extraerlas (los DNI/RUC se capturan por regex, sin llamadas
     * extra de IA, para no demorar la conversación ni gastar tokens).
     */
    private static final java.util.regex.Pattern BUSINESS_DETAILS_TRIGGER = java.util.regex.Pattern.compile(
            "\\b(raz[oó]n\\s+social|direcci[oó]n|av\\.|calle|jr\\.|urb\\.|mz\\.|lt\\.|empresa|s\\.?a\\.?c\\.?"
                    + "|eirl|srl|ltda)\\b",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Extrae los datos de facturación del cliente (DNI/RUC, nombre o razón
     * social, dirección) desde sus mensajes y los guarda en su contacto para
     * que las cotizaciones en PDF salgan con datos formales. El proceso nunca
     * interrumpe el flujo normal del chat: ante cualquier error solo registra
     * el aviso.
     */
    private void captureContactDetails(String message, Map<String, Object> context) {
        if (message == null || message.isBlank()
                || !TAX_DETAILS_TRIGGER.matcher(message.toLowerCase()).find()) {
            return;
        }
        String contactIdStr = (String) context.get("contactId");
        if (contactIdStr == null) {
            return;
        }
        try {
            UUID tenantId = getTenantId();
            UUID contactId = UUID.fromString(contactIdStr);
            Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(contactId, tenantId)
                    .orElse(null);
            if (contact == null) {
                return;
            }
            if (applyTaxInfo(contact, extractTaxInfo(message))) {
                contactRepository.save(contact);
                log.info("Tax details captured for contact {}", contactId);
            }
        } catch (Exception e) {
            log.warn("Could not capture tax details from message: {}", e.getMessage());
        }
    }

    private record ContactTaxInfo(String documentType, String documentNumber, String fullName, String address) {
    }

    /**
     * Extrae los datos fiscales: DNI/RUC siempre por regex (instantáneo, sin
     * IA). El LLM solo se invoca si el mensaje sugiere razón social o
     * dirección, que no pueden capturarse de forma confiable por patrones.
     */
    private ContactTaxInfo extractTaxInfo(String message) {
        ContactTaxInfo byRegex = extractTaxInfoByRegex(message);
        if (!BUSINESS_DETAILS_TRIGGER.matcher(message.toLowerCase()).find()) {
            return byRegex;
        }
        try {
            AIMessageDto aiMessage = AIMessageDto.builder()
                    .role("user")
                    .content(message)
                    .build();
            AIResponseDto response = aiProvider.chatCompletion(
                    List.of(aiMessage),
                    "Eres un extractor de datos de facturación peruana. Del mensaje del cliente "
                            + "extrae: documentType (solo \"DNI\" o \"RUC\"), documentNumber (solo "
                            + "dígitos), fullName (nombre completo o razón social) y address "
                            + "(dirección). Responde ÚNICAMENTE con un JSON válido, sin texto "
                            + "adicional, con este formato: "
                            + "{\"documentType\":\"DNI\",\"documentNumber\":\"12345678\","
                            + "\"fullName\":\"...\",\"address\":\"...\"}. Usa null para lo que "
                            + "no aparezca en el mensaje.",
                    0.0, null);
            if (response == null || response.getContent() == null || response.getContent().isBlank()) {
                return byRegex;
            }
            String content = response.getContent().replaceAll("```json|```", "").trim();
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return byRegex;
            }
            JsonNode node = objectMapper.readTree(content.substring(start, end + 1));
            return new ContactTaxInfo(
                    node.hasNonNull("documentType") ? node.get("documentType").asText() : null,
                    node.hasNonNull("documentNumber") ? node.get("documentNumber").asText() : null,
                    node.hasNonNull("fullName") ? node.get("fullName").asText() : null,
                    node.hasNonNull("address") ? node.get("address").asText() : null);
        } catch (Exception e) {
            log.warn("LLM tax extraction failed, using regex fallback: {}", e.getMessage());
            return byRegex;
        }
    }

    /**
     * Captura RUC (11 dígitos) o DNI (8 dígitos) directamente del mensaje,
     * sin llamar al LLM.
     */
    private ContactTaxInfo extractTaxInfoByRegex(String message) {
        String text = message.toLowerCase();
        Matcher ruc = java.util.regex.Pattern.compile("\\b\\d{11}\\b").matcher(text);
        if (ruc.find()) {
            return new ContactTaxInfo("RUC", ruc.group(), null, null);
        }
        Matcher dni = java.util.regex.Pattern.compile("\\b\\d{8}\\b").matcher(text);
        if (dni.find()) {
            return new ContactTaxInfo("DNI", dni.group(), null, null);
        }
        return new ContactTaxInfo(null, null, null, null);
    }

    /**
     * Aplica al contacto los datos fiscales extraídos, sin sobreescribir
     * información ya registrada. Devuelve true si algo cambió.
     */
    private boolean applyTaxInfo(Contact contact, ContactTaxInfo info) {
        boolean changed = false;

        String docNumber = cleanDigits(info.documentNumber());
        String docType = normalizeDocumentType(info.documentType(), docNumber);

        if (docNumber != null && docNumber.length() == 11 && !docNumber.equals(contact.getDocumentNumber())) {
            contact.setDocumentNumber(docNumber);
            changed = true;
        } else if (docNumber != null && docNumber.length() == 8 && !docNumber.equals(contact.getDocumentNumber())) {
            contact.setDocumentNumber(docNumber);
            changed = true;
        }
        if (docType != null && !docType.equals(contact.getDocumentType())) {
            contact.setDocumentType(docType);
            changed = true;
        }

        if (hasText(info.fullName())) {
            String fullName = info.fullName().trim();
            if ("RUC".equals(contact.getDocumentType())) {
                if (!hasText(contact.getCompany()) || !contact.getCompany().equalsIgnoreCase(fullName)) {
                    contact.setCompany(fullName);
                    changed = true;
                }
            } else if (!hasText(contact.getFirstName()) && !hasText(contact.getLastName())) {
                String[] parts = fullName.split("\\s+", 2);
                contact.setFirstName(parts[0]);
                if (parts.length > 1) {
                    contact.setLastName(parts[1]);
                }
                contact.updateFullName();
                changed = true;
            }
        }

        if (hasText(info.address()) && !info.address().trim().equals(contact.getAddress())) {
            contact.setAddress(info.address().trim());
            changed = true;
        }

        return changed;
    }

    private String cleanDigits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return (digits.length() == 8 || digits.length() == 11) ? digits : null;
    }

    private String normalizeDocumentType(String docType, String docNumber) {
        if (docType != null) {
            String normalized = docType.trim().toUpperCase();
            if ("DNI".equals(normalized) || "RUC".equals(normalized)) {
                return normalized;
            }
        }
        if (docNumber != null) {
            return docNumber.length() == 11 ? "RUC" : "DNI";
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String[] extractKeywords(String json) {
        if (json == null || json.isBlank()) return new String[0];
        try {
            var arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                String[] result = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    result[i] = arr.get(i).asText();
                }
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid keywords JSON: {}", e.getMessage());
        }
        return new String[0];
    }

    private String[] extractResponses(String json) {
        if (json == null || json.isBlank()) return new String[0];
        try {
            var arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                String[] result = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    result[i] = arr.get(i).asText();
                }
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid responses JSON: {}", e.getMessage());
        }
        return new String[0];
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
