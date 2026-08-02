package com.iquenobot.orchestrator.application.aggregation;

import com.iquenobot.conversation.application.ConversationHandoffService;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.shared.domain.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Scheduler de consolidación de respuestas automáticas.
 *
 * Cada segundo busca conversaciones marcadas como pendientes cuya última
 * actividad superó la ventana de debounce (4s) y las procesa una por una.
 *
 * Thread-safe: el procesamiento por conversación se serializa con
 * FOR UPDATE SKIP LOCKED, por lo que múltiples instancias del scheduler
 * (o reinicios) nunca procesan la misma conversación dos veces.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PendingAiResponseScheduler {

    private final ConversationRepository conversationRepository;
    private final MessageAggregationService aggregationService;
    private final ConversationOrchestrator orchestrator;
    private final ConversationHandoffService handoffService;

    @Scheduled(fixedDelayString = "${app.ai.pending-response-poll-ms:1000}")
    public void processPendingConversations() {
        LocalDateTime threshold = LocalDateTime.now(ZoneOffset.UTC)
                .minusSeconds(MessageAggregationService.DEBOUNCE_WINDOW_SECONDS);

        List<Conversation> pending = conversationRepository.findPendingAiConversations(threshold);
        if (pending.isEmpty()) {
            return;
        }

        log.info("Pending AI responses: {} conversation(s) to consolidate", pending.size());

        for (Conversation conversation : pending) {
            try {
                // El tenant se resuelve por conversación: nunca se mezclan tenants.
                TenantContext.setTenantId(conversation.getTenantId().toString());

                // Mismo lock que el flujo de webhooks: el pipeline síncrono y la
                // consolidación nunca tocan la misma conversación en paralelo.
                String lockKey = ConversationOrchestrator.normalizeLockKey(
                        conversation.getChannel() != null ? conversation.getChannel().name() : "UNKNOWN",
                        conversation.getChannelConversationId() != null
                                ? conversation.getChannelConversationId()
                                : conversation.getId().toString());

                orchestrator.withConversationLock(lockKey, () -> {
                    handoffService.resumeBotIfAgentIdle(conversation);
                    aggregationService.processPendingConversation(conversation);
                    return null;
                });
            } catch (Exception e) {
                log.error("Error consolidating pending conversation {}: {}",
                        conversation.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
