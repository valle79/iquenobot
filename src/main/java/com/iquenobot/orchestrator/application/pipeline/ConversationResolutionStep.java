package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.orchestrator.interfaces.event.ConversationCreatedEvent;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.shared.enums.ConversationPriority;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationResolutionStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final EventPublisher eventPublisher;

    @Override
    public int getOrder() { return 30; }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {
        var message = context.getIncomingMessage();
        UUID tenantId = context.getTenantId();
        Contact contact = context.getContact();

        String channelConversationId = buildChannelConversationId(message);

        Conversation conversation = conversationRepository
                .findByChannelConversationIdAndTenantIdAndDeletedFalse(channelConversationId, tenantId)
                .orElseGet(() -> createConversation(context, channelConversationId));

        if (message.getConversationName() != null && !message.getConversationName().isBlank()) {
            conversation.setSubject(message.getConversationName());
            conversation = conversationRepository.save(conversation);
        }

        context.setConversation(conversation);
        context.setChannelConversationId(channelConversationId);

        log.debug("Conversation resolved: id={} status={} channel={}",
                conversation.getId(), conversation.getStatus(), conversation.getChannel());
        return context;
    }

    private String buildChannelConversationId(IncomingMessage message) {
        String conversationKey = message.getChannelConversationId();
        if (conversationKey == null || conversationKey.isBlank()) {
            conversationKey = message.getSourceIdentifier();
        }
        String normalized = conversationKey.replaceAll("[^0-9]", "");
        return message.getChannel().name() + ":" + normalized;
    }

    private Conversation createConversation(ProcessingContext context, String channelConversationId) {
        var message = context.getIncomingMessage();
        Contact contact = context.getContact();

        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .contact(contact)
                .channel(message.getChannel())
                .status(ConversationStatus.OPEN)
                .priority(ConversationPriority.MEDIUM)
                .channelConversationId(channelConversationId)
                .lastMessageAt(LocalDateTime.now())
                .messageCount(0)
                .unreadCount(0)
                .botConversation(false)
                .build();

        conversation = conversationRepository.save(conversation);

        contact.incrementConversationCount();
        contactRepository.save(contact);

        eventPublisher.publish(new ConversationCreatedEvent(
                context.getTenantId().toString(),
                conversation.getId().toString(),
                contact.getId().toString(),
                message.getChannel().name()
        ));

        log.info("New conversation created: id={} contact={} channel={}",
                conversation.getId(), contact.getId(), message.getChannel());
        return conversation;
    }
}
