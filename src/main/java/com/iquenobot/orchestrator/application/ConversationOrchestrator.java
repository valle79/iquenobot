package com.iquenobot.orchestrator.application;

import com.iquenobot.orchestrator.application.action.ActionDispatcher;
import com.iquenobot.orchestrator.application.pipeline.MessagePipeline;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.domain.service.DecisionEngine;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.ActionExecutedEvent;
import com.iquenobot.orchestrator.interfaces.event.MessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationOrchestrator {

    private static final String CORRELATION_ID = "correlationId";
    private static final String TENANT_ID = "tenantId";
    private static final String CONVERSATION_ID = "conversationId";
    private static final String CONTACT_ID = "contactId";
    private static final String CHANNEL = "channel";
    private static final String MESSAGE_ID = "messageId";

    // Serializa el procesamiento por conversación para evitar optimistic locks
    // y respuestas desordenadas cuando llegan ráfagas de webhooks (redelivery).
    private final ConcurrentHashMap<String, ReentrantLock> conversationLocks = new ConcurrentHashMap<>();

    private final MessagePipeline pipeline;
    private final DecisionEngine decisionEngine;
    private final ActionDispatcher actionDispatcher;
    private final EventPublisher eventPublisher;

    public ProcessingResult processMessage(IncomingMessage message) {
        long startTime = System.currentTimeMillis();
        String correlationId = message.getChannelMessageId() != null
                ? message.getChannelMessageId()
                : UUID.randomUUID().toString();

        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(CHANNEL, message.getChannel() != null ? message.getChannel().name() : "UNKNOWN");
        MDC.put(MESSAGE_ID, correlationId);

        log.info("Orchestrator processing message from channel={} source={}",
                message.getChannel(), message.getSourceIdentifier());

        // Clave de serialización: canal + identificador de la conversación remota
        // normalizado a dígitos. El PendingAiResponseScheduler reconstruye la
        // MISMA clave desde la conversación persistida, de modo que el flujo
        // síncrono del webhook y la consolidación nunca tocan la conversación
        // simultáneamente (elimina optimistic locks y deadlocks).
        String lockKey = normalizeLockKey(
                message.getChannel() != null ? message.getChannel().name() : "UNKNOWN",
                message.getChannelConversationId() != null
                        ? message.getChannelConversationId()
                        : message.getSourceIdentifier());
        ReentrantLock lock = conversationLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        boolean locked = lock.tryLock();

        if (!locked) {
            log.info("Message from {} already being processed, waiting in queue", lockKey);
            lock.lock();
        }

        try {
            return doProcessMessage(message, startTime, correlationId);
        } finally {
            lock.unlock();
            if (conversationLocks.size() > 1000) {
                conversationLocks.remove(lockKey);
            }
            MDC.clear();
        }
    }

    /**
     * Ejecuta una acción bajo el mismo lock de conversación que el flujo de
     * webhooks, para que el scheduler de consolidación y el pipeline nunca
     * modifiquen la misma conversación en paralelo dentro de la JVM.
     */
    public <T> T withConversationLock(String lockKey, java.util.function.Supplier<T> action) {
        ReentrantLock lock = conversationLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        boolean locked = lock.tryLock();

        if (!locked) {
            log.debug("Conversation {} busy with webhook processing, waiting", lockKey);
            lock.lock();
        }

        try {
            return action.get();
        } finally {
            lock.unlock();
            if (conversationLocks.size() > 1000) {
                conversationLocks.remove(lockKey);
            }
        }
    }

    /**
     * Normaliza la clave de lock al mismo formato que la conversación
     * persistida usa en channel_conversation_id (canal + solo dígitos),
     * garantizando que webhook y scheduler calculen claves idénticas.
     */
    public static String normalizeLockKey(String channel, String rawIdentifier) {
        if (rawIdentifier == null) {
            rawIdentifier = "";
        }
        return channel + ":" + rawIdentifier.replaceAll("[^0-9]", "");
    }

    private ProcessingResult doProcessMessage(IncomingMessage message, long startTime, String correlationId) {
        MDC.put(CORRELATION_ID, correlationId);
        MDC.put(CHANNEL, message.getChannel() != null ? message.getChannel().name() : "UNKNOWN");
        MDC.put(MESSAGE_ID, correlationId);

        log.info("Orchestrator processing message from channel={} source={}",
                message.getChannel(), message.getSourceIdentifier());

        try {
            ProcessingContext context = new ProcessingContext();
            context.setIncomingMessage(message);

            context = pipeline.execute(context);

            if (context.getTenantId() != null) MDC.put(TENANT_ID, context.getTenantId().toString());
            if (context.getConversation() != null) MDC.put(CONVERSATION_ID, context.getConversation().getId().toString());
            if (context.getContact() != null) MDC.put(CONTACT_ID, context.getContact().getId().toString());

            eventPublisher.publish(new MessageReceivedEvent(
                    context.getTenantId() != null ? context.getTenantId().toString() : null,
                    context.getConversation() != null ? context.getConversation().getId().toString() : null,
                    message
            ));

            // -----------------------------------------------------------------
            // Mensajes entrantes marcados como pendientes de IA: la respuesta
            // (y la asignación de agente) la ejecuta PendingAiResponseScheduler
            // tras la ventana de debounce. Aquí NO se decide nada para no
            // mutar la conversación en paralelo con la consolidación.
            // -----------------------------------------------------------------
            boolean deferred = !message.isOutbound()
                    && context.getConversation() != null
                    && context.getConversation().isPendingAiResponse();

            Decision decision;
            if (deferred) {
                log.info("Conversation {} is pending AI response; scheduler will handle it "
                        + "(NO_ACTION = DEFERRED, esperando ventana de consolidación)",
                        context.getConversation().getId());
                decision = Decision.builder()
                        .actionType(ActionType.NO_ACTION)
                        .reason("DEFERRED: waiting for 4s consolidation window")
                        .build();
            } else {
                decision = decisionEngine.decide(context);
            }

            actionDispatcher.dispatch(decision, context);

            for (Decision secondary : context.getSecondaryDecisions()) {
                actionDispatcher.dispatch(secondary, context);
            }

            eventPublisher.publish(new ActionExecutedEvent(
                    context.getTenantId() != null ? context.getTenantId().toString() : null,
                    context.getConversation() != null ? context.getConversation().getId().toString() : null,
                    decision,
                    message,
                    System.currentTimeMillis() - startTime
            ));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Message processed successfully in {}ms. decision={}",
                    elapsed, decision.getActionType());

            return ProcessingResult.success(decision, "Message processed successfully", elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Message processing failed after {}ms: {}",
                    elapsed, e.getMessage(), e);
            return ProcessingResult.failure("PROCESSING_ERROR", e.getMessage(), elapsed);
        }
    }
}
