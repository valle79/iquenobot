package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.BotConfiguration;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BotDecisionStrategy implements DecisionStrategy {

    private final ChatbotService chatbotService;
    private final ConversationRepository conversationRepository;

    private record LeadRule(String title, int baseScore) {}

    private static final Map<String, LeadRule> COMMERCIAL_RULES = Map.of(
            "solicitar_precio", new LeadRule("Solicitud de cotización", 40),
            "consulta_envio", new LeadRule("Consulta de envío", 30),
            "forma_pago", new LeadRule("Interés en compra - forma de pago", 35),
            "informacion_empresa", new LeadRule("Interés general en productos/servicios", 25)
    );

    /** Número máximo de intentos de aclaración antes de pasar la conversación al agente. */
    private static final int MAX_CLARIFICATION_ATTEMPTS = 1;

    @Override
    public int getPriority() { return 10; }

    @Override
    public boolean canHandle(ProcessingContext context) {
        if (context.getIncomingMessage().isOutbound()) {
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
        if (!conversation.isBotConversation() && conversation.getAssignedUser() != null) {
            log.debug("Conversation {} is assigned to an agent; bot will respond anyway", conversation.getId());
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
                            "systemPrompt", botConfig != null ? botConfig.getSystemPrompt() : "",
                            "fallbackMessage", botConfig != null ? botConfig.getFallbackMessage() : "",
                            "temperature", String.valueOf(botConfig != null ? botConfig.getTemperature() : 0.7),
                            "isFirstMessage", isFirstMessage
                    )
            );

            if (botResponse.isRequiresHumanAgent()) {
                resetFallbackCount(context);
                return Decision.builder()
                        .actionType(ActionType.TRANSFER_CONVERSATION)
                        .reason("Customer requested human agent or bot error")
                        .requiresAgent(true)
                        .parameters(Map.of(
                                "response", botResponse.getMessage(),
                                "intent", botResponse.getIntentDetected() != null
                                        ? botResponse.getIntentDetected() : "unknown"
                        ))
                        .build();
            }

            if (botResponse.isRequiresClarification()) {
                return handleClarification(context, botResponse);
            }

            resetFallbackCount(context);

            String intent = botResponse.getIntentDetected() != null
                    ? botResponse.getIntentDetected() : "unknown";

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
                                "messageContent", msg.getContent()
                        ))
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
                                "intent", intent
                        ))
                        .build();
            }

            return Decision.builder()
                    .actionType(ActionType.SEND_TEXT)
                    .reason("Bot responded to message")
                    .parameters(Map.of(
                            "response", botResponse.getMessage(),
                            "intent", intent,
                            "flowExecuted", botResponse.getFlowExecuted() != null
                                    ? botResponse.getFlowExecuted() : ""
                    ))
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
                            "intent", "unknown"
                    ))
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
                        "intent", "unknown"
                ))
                .build();
    }

    private boolean hasPurchaseIntent(ProcessingContext context, String intent) {
        String content = context.getIncomingMessage().getContent();
        return content != null && ChatbotService.PURCHASE_INTENT_PATTERN.matcher(content.toLowerCase()).find();
    }

    private void resetFallbackCount(ProcessingContext context) {
        var conversation = context.getConversation();
        if (conversation.getBotFallbackCount() != 0) {
            conversation.setBotFallbackCount(0);
            conversationRepository.save(conversation);
        }
    }
}
