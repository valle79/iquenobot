package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.conversation.application.ConversationHandoffService;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.entity.MessageAttachment;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.conversation.domain.repository.MessageAttachmentRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.shared.enums.AttachmentType;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePersistenceStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    /**
     * Ventana de entrega aceptada para mensajes de WhatsApp. Un mensaje entrante
     * cuyo timestamp sea más antiguo que este lapso respecto a AHORA se considera
     * una redelivery histórica (Evolution reentrega mensajes viejos tras una
     * reconexión/reinicio): se persiste para no perder el historial, pero el bot
     * no responde automáticamente por él.
     */
    private static final Duration STALE_DELIVERY_WINDOW = Duration.ofMinutes(10);

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final ConversationHandoffService handoffService;
    private final UserRepository userRepository;

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {
        var message = context.getIncomingMessage();
        Conversation conversation = context.getConversation();

        boolean outbound = message.isOutbound();

        ConversationMessage persisted = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .conversation(conversation)
                .direction(outbound ? MessageDirection.OUTBOUND : MessageDirection.INBOUND)
                .senderType(outbound ? SenderType.AGENT : SenderType.CUSTOMER)
                .type(message.getType())
                .status(MessageStatus.SENT)
                .content(message.getContent())
                .channelMessageId(message.getChannelMessageId())
                .senderName(resolveSenderName(conversation, message, context.getContact()))
                .senderPhone(context.getContact().getPhone())
                .fromBot(false)
                .sentAt(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now(ZoneOffset.UTC))
                .build();

        persisted = messageRepository.save(persisted);

        if (message.getMediaUrl() != null && !message.getMediaUrl().isBlank()) {
            MessageAttachment attachment = MessageAttachment.builder()
                    .id(UUID.randomUUID())
                    .tenantId(context.getTenantId())
                    .message(persisted)
                    .type(resolveAttachmentType(message.getType()))
                    .fileName(message.getFilename())
                    .fileUrl(message.getMediaUrl())
                    .mimeType(message.getMimeType())
                    .caption(message.getCaption())
                    .channelMediaId(message.getChannelMediaId())
                    .durationSeconds(message.getDurationSeconds())
                    .build();
            attachmentRepository.save(attachment);
            log.debug("Attachment persisted: message={} url={} type={}",
                    persisted.getId(), message.getMediaUrl(), attachment.getType());
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        conversationRepository.incrementIncomingMessageMetrics(
                conversation.getId(), context.getTenantId(), now);

        // ---------------------------------------------------------------------
        // DEBOUNCE: los mensajes entrantes no ejecutan IA aquí.
        // Solo se marca la conversación como pendiente; el
        // PendingAiResponseScheduler la consolida y responde después.
        // ---------------------------------------------------------------------
        if (!outbound) {
            // Mensaje del cliente → marcar pendiente de IA. EXCEPCIÓN: mensajes
            // de WhatsApp entregados con demora (redelivery de Evolution tras un
            // reinicio): se persisten para no perder historial, pero NO se marcan
            // pendientes, de forma que el bot no responda en automático mensajes
            // viejos. Quedan visibles en el sistema para el agente.
            if (isStaleDeliveredMessage(message)) {
                log.info("Stale WhatsApp message persisted without AI response: "
                                + "conversation={} msgId={} timestamp={}",
                        conversation.getId(), message.getChannelMessageId(), message.getTimestamp());
            } else {
                conversation.setPendingAiResponse(true);
                conversationRepository.save(conversation);
                log.debug("Conversation {} marked as pending AI response", conversation.getId());
            }
        } else {
            // Mensaje del agente (desde celular o panel vía webhook) → pausar bot
            UUID agentId = conversation.getAssignedUser() != null
                    ? conversation.getAssignedUser().getId()
                    : null;

            handoffService.onAgentMessage(conversation, agentId);

            log.info("Outbound agent message via webhook → human handoff activated for conversation {}",
                    conversation.getId());
        }

        context.setPersistedMessage(persisted);

        log.debug("Message persisted: id={} conversation={} type={}",
                persisted.getId(), conversation.getId(), message.getType());
        return context;
    }

    private boolean isStaleDeliveredMessage(IncomingMessage message) {
        if (message == null || message.getChannel() != ChannelType.WHATSAPP
                || message.getTimestamp() == null) {
            return false;
        }
        return message.getTimestamp()
                .isBefore(LocalDateTime.now(ZoneOffset.UTC).minus(STALE_DELIVERY_WINDOW));
    }

    /**
     * Resuelve el nombre visible del remitente:
     * - Mensajes del agente (OUTBOUND): el nombre del agente asignado a la
     *   conversación, o el pushName del webhook como respaldo.
     * - Mensajes del cliente (INBOUND): el nombre del contacto (o del grupo).
     */
    private String resolveSenderName(
            Conversation conversation,
            IncomingMessage message,
            com.iquenobot.contact.domain.entity.Contact contact
    ) {
        if (message.isOutbound()) {
            String agentName = resolveAssignedAgentName(conversation);
            if (agentName != null && !agentName.isBlank()) {
                return agentName;
            }
            if (message.getSourceName() != null && !message.getSourceName().isBlank()) {
                return message.getSourceName();
            }
            return "Agente";
        }
        return conversation.isGroup()
                ? message.getSourceName()
                : contact.getFullName();
    }

    /**
     * Nombre del agente asignado a la conversación.
     *
     * El conversation llega al pipeline DESACOPLADO de su sesión de Hibernate
     * (se carga en un paso anterior del pipeline), por lo que acceder a
     * assignedUser.getFullName() lanza LazyInitializationException. Se resuelve
     * el usuario dentro de esta transacción activa con findById (nunca navega
     * por el proxy detached).
     */
    private String resolveAssignedAgentName(Conversation conversation) {
        if (conversation == null || conversation.getAssignedUser() == null) {
            return null;
        }
        UUID agentId = conversation.getAssignedUser().getId();
        if (agentId == null) {
            return null;
        }
        return userRepository.findById(agentId)
                .map(User::getFullName)
                .orElse(null);
    }

    private AttachmentType resolveAttachmentType(MessageType type) {
        if (type == null) {
            return AttachmentType.DOCUMENT;
        }
        return switch (type) {
            case IMAGE -> AttachmentType.IMAGE;
            case VIDEO -> AttachmentType.VIDEO;
            case AUDIO -> AttachmentType.AUDIO;
            case STICKER -> AttachmentType.STICKER;
            case LOCATION -> AttachmentType.LOCATION;
            case CONTACT -> AttachmentType.CONTACT;
            default -> AttachmentType.DOCUMENT;
        };
    }
}
