package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
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

    private record LeadRule(String title, int baseScore) {}

    private static final Map<String, LeadRule> COMMERCIAL_RULES = Map.of(
            "solicitar_precio", new LeadRule("Solicitud de cotización", 40),
            "consulta_envio", new LeadRule("Consulta de envío", 30),
            "forma_pago", new LeadRule("Interés en compra - forma de pago", 35),
            "informacion_empresa", new LeadRule("Interés general en productos/servicios", 25)
    );

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

        if (context.getConversation().isBotConversation()) {
            return true;
        }
        if (context.getConversation().getAssignedUser() == null) {
            return true;
        }
        return false;
    }

    @Override
    public Decision decide(ProcessingContext context) {
        var msg = context.getIncomingMessage();
        var botConfig = context.getBotConfiguration();

        try {
            var conversation = context.getConversation();
            boolean isFirstMessage = conversation.getMessageCount() == 0;

            ChatbotResponseDto botResponse = chatbotService.processMessage(
                    msg.getContent(),
                    Map.of(
                            "conversationId", conversation.getId().toString(),
                            "tenantId", context.getTenantId().toString(),
                            "contactId", context.getContact().getId().toString(),
                            "channel", msg.getChannel().name(),
                            "systemPrompt", botConfig != null ? botConfig.getSystemPrompt() : "",
                            "temperature", String.valueOf(botConfig != null ? botConfig.getTemperature() : 0.7),
                            "isFirstMessage", isFirstMessage
                    )
            );

            if (botResponse.isRequiresHumanAgent()) {
                return Decision.builder()
                        .actionType(ActionType.TRANSFER_CONVERSATION)
                        .reason("Bot requested human agent handoff")
                        .requiresAgent(true)
                        .parameters(Map.of(
                                "response", botResponse.getMessage(),
                                "intent", botResponse.getIntentDetected() != null
                                        ? botResponse.getIntentDetected() : "unknown"
                        ))
                        .build();
            }

            String intent = botResponse.getIntentDetected() != null
                    ? botResponse.getIntentDetected() : "unknown";

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
}
