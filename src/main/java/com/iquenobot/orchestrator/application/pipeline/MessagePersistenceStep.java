package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessagePersistenceStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public int getOrder() { return 40; }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {
        var message = context.getIncomingMessage();
        Conversation conversation = context.getConversation();

        ConversationMessage persisted = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .conversation(conversation)
                .direction(MessageDirection.INBOUND)
                .type(message.getType())
                .status(MessageStatus.SENT)
                .content(message.getContent())
                .channelMessageId(message.getChannelMessageId())
                .senderName(context.getContact().getFullName())
                .senderPhone(context.getContact().getPhone())
                .fromBot(false)
                .sentAt(message.getTimestamp() != null ? message.getTimestamp() : LocalDateTime.now())
                .build();

        persisted = messageRepository.save(persisted);

        conversation.incrementMessageCount();
        conversation.incrementUnreadCount();
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        context.setPersistedMessage(persisted);

        log.debug("Message persisted: id={} conversation={} type={}",
                persisted.getId(), conversation.getId(), message.getType());
        return context;
    }
}
