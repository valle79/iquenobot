package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.entity.MessageAttachment;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.conversation.domain.repository.MessageAttachmentRepository;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.shared.enums.AttachmentType;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePersistenceStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageAttachmentRepository attachmentRepository;

    @Override
    public int getOrder() { return 40; }

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
                .senderName(conversation.isGroup()
                        ? message.getSourceName()
                        : context.getContact().getFullName())
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
            conversation.setPendingAiResponse(true);
            conversationRepository.save(conversation);
            log.debug("Conversation {} marked as pending AI response", conversation.getId());
        }

        context.setPersistedMessage(persisted);

        log.debug("Message persisted: id={} conversation={} type={}",
                persisted.getId(), conversation.getId(), message.getType());
        return context;
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
