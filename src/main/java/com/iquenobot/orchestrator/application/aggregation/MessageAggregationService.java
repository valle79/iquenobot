package com.iquenobot.orchestrator.application.aggregation;

import com.iquenobot.conversation.application.ConversationHandoffService;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.application.action.AssignAgentExecutor;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Consolida los mensajes entrantes no procesados de una conversación en una
 * única interacción lógica (debounce de 4s) y dispara una única respuesta
 * automática, con validaciones anti-duplicados (hash del contenido y ventana
 * mínima de 10s entre respuestas del bot).
 *
 * Thread-safe: la conversación se reclama con bloqueo pesimista
 * (FOR UPDATE SKIP LOCKED) para que dos instancias del scheduler nunca
 * procesen la misma conversación simultáneamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageAggregationService {

    /** Ventana de debounce: agrupa mensajes del contacto llegados en este lapso. */
    public static final int DEBOUNCE_WINDOW_SECONDS = 4;

    /** Ventana mínima entre respuestas automáticas del bot (anti-spam). */
    public static final int MIN_BOT_RESPONSE_INTERVAL_SECONDS = 10;

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final AiResponseService aiResponseService;
    private final AssignAgentExecutor assignAgentExecutor;
    private final ConversationHandoffService handoffService;

    /**
     * Reclama la conversación pendiente con bloqueo pesimista de fila
     * (FOR UPDATE SKIP LOCKED) y, solo si el reclamo fue exitoso, la consolida.
     *
     * El reclamo se hace ANTES de cualquier reanudación del bot o del
     * procesamiento, dentro de la misma transacción: si otra instancia o hilo
     * del scheduler ya la reclamó, esta llamada no espera y no procesa nada,
     * garantizando que una conversación jamás genera dos respuestas.
     *
     * Es el único punto de entrada del scheduler hacia la consolidación.
     */
    @Transactional
    public void claimAndProcessPending(Conversation candidate) {
        Optional<Conversation> claimed = conversationRepository
                .findPendingConversationForUpdate(candidate.getId());
        if (claimed.isEmpty()) {
            log.debug("Conversation {} already claimed by another worker", candidate.getId());
            return;
        }
        Conversation conversation = claimed.get();

        if (!conversation.isPendingAiResponse()) {
            return;
        }

        // Handoff humano: si un agente intervino y está dentro de su ventana de
        // atención, el bot se pausa (al vencer la ventana, canBotRespond
        // reactiva el bot automáticamente y el próximo tick consolida).
        if (!handoffService.canBotRespond(conversation)) {
            return;
        }

        processPendingConversation(conversation);
    }

    /**
     * Consolida los mensajes entrantes no procesados de una conversación en una
     * única interacción lógica (debounce de 4s) y dispara una única respuesta
     * automática, con validaciones anti-duplicados (hash del contenido y ventana
     * mínima de 10s entre respuestas del bot).
     *
     * La conversación ya debe estar reclamada (ver {@link #claimAndProcessPending}).
     */
    @Transactional
    public void processPendingConversation(Conversation conversation) {

        // -----------------------------------------------------------------
        // 1. Mensajes entrantes aún no consumidos (orden cronológico)
        // -----------------------------------------------------------------
        List<ConversationMessage> pendingMessages =
                messageRepository.findUnprocessedInboundMessages(conversation.getId());
        if (pendingMessages.isEmpty()) {
            conversation.setPendingAiResponse(false);
            conversationRepository.save(conversation);
            return;
        }

        // -----------------------------------------------------------------
        // 2. Ventana de debounce: esperar 4s desde la última actividad del
        //    contacto para agrupar todos los mensajes de la ráfaga.
        // -----------------------------------------------------------------
        LocalDateTime lastActivity = conversation.getLastMessageAt() != null
                ? conversation.getLastMessageAt()
                : pendingMessages.get(pendingMessages.size() - 1).getSentAt();
        if (lastActivity != null
                && lastActivity.plusSeconds(DEBOUNCE_WINDOW_SECONDS)
                        .isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            log.debug("Conversation {} still inside debounce window; waiting", conversation.getId());
            return;
        }

        // -----------------------------------------------------------------
        // 3. Consolidación del texto
        // -----------------------------------------------------------------
        String consolidated = consolidate(pendingMessages);
        if (consolidated.isBlank()) {
            log.info("Conversation {} has no textual content to respond to; marking processed",
                    conversation.getId());
            markProcessed(conversation, pendingMessages, null, false);
            return;
        }

        // -----------------------------------------------------------------
        // 4. Validación anti-duplicados
        // -----------------------------------------------------------------
        String hash = sha256(consolidated);

        if (hash.equals(conversation.getLastProcessedMessageHash())) {
            log.info("Conversation {} duplicate consolidated content; skipping response",
                    conversation.getId());
            markProcessed(conversation, pendingMessages, hash, false);
            return;
        }

        if (conversation.getLastBotResponseAt() != null
                && Duration.between(
                        conversation.getLastBotResponseAt(),
                        LocalDateTime.now(ZoneOffset.UTC)
                ).getSeconds() < MIN_BOT_RESPONSE_INTERVAL_SECONDS) {
            log.info("Conversation {} answered by bot < {}s ago; keeping pending to avoid spam",
                    conversation.getId(), MIN_BOT_RESPONSE_INTERVAL_SECONDS);
            return;
        }

        // -----------------------------------------------------------------
        // 5. Respuesta automática (una única por lote consolidado)
        // -----------------------------------------------------------------
        aiResponseService.generateAndSendResponse(
                conversation.getTenantId(), conversation, consolidated);

        // -----------------------------------------------------------------
        // 6. Flags post-respuesta, todo en la misma transacción
        // -----------------------------------------------------------------
        markProcessed(conversation, pendingMessages, hash, true);

        // -----------------------------------------------------------------
        // 7. Asignación de agente (mismo criterio que el flujo síncrono
        //    original: conversación OPEN sin agente). Ahora el webhook ya
        //    no la ejecuta para no competir con la consolidación.
        // -----------------------------------------------------------------
        if (conversation.getStatus() == ConversationStatus.OPEN) {
            assignAgentExecutor.assignIfUnassigned(
                    conversation, conversation.getTenantId(), conversation.getContact().getId());
        }
    }

    private String consolidate(List<ConversationMessage> messages) {
        return messages.stream()
                .map(ConversationMessage::getContent)
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }

    private void markProcessed(Conversation conversation, List<ConversationMessage> messages,
                               String hash, boolean responded) {
        conversation.setPendingAiResponse(false);
        conversation.setLastProcessedMessageHash(hash);
        if (responded) {
            conversation.setLastBotResponseAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        conversationRepository.save(conversation);

        for (ConversationMessage message : messages) {
            message.setAiProcessed(true);
        }
        messageRepository.saveAll(messages);

        log.info("Conversation {} consolidated: {} message(s) processed, bot responded={}",
                conversation.getId(), messages.size(), responded);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
