
package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.orchestrator.interfaces.event.ConversationCreatedEvent;
import com.iquenobot.shared.enums.ConversationPriority;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationResolutionStep
        implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final EventPublisher eventPublisher;

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {

        IncomingMessage message = context.getIncomingMessage();
        UUID tenantId = context.getTenantId();
        Contact contact = context.getContact();

        String channelConversationId = buildChannelConversationId(message);

        Conversation conversation = resolveConversation(
                context,
                channelConversationId,
                contact
        );

        updateConversationMetadata(conversation, message);

        context.setConversation(conversation);
        context.setChannelConversationId(channelConversationId);

        log.debug(
                "Conversation resolved: id={} status={} channel={} conversationKey={}",
                conversation.getId(),
                conversation.getStatus(),
                conversation.getChannel(),
                channelConversationId
        );

        return context;
    }

    /**
     * Construye un identificador estable para la conversación.
     *
     * Ejemplos:
     * - WHATSAPP:120363424364570024@g.us
     * - WHATSAPP:51972349298@s.whatsapp.net
     */
@SuppressWarnings("StringSplitter")
private String buildChannelConversationId(IncomingMessage message) {

    String conversationKey = message.getChannelConversationId();

    // Para mensajes individuales usamos el sourceIdentifier
    if (!message.isGroup()) {
        conversationKey = message.getSourceIdentifier();
    }

    if (conversationKey == null || conversationKey.isBlank()) {
        throw new IllegalStateException(
                "WhatsApp message without conversation identifier"
        );
    }

    // -------------------------------------------------------------
    // NORMALIZACIÓN
    // -------------------------------------------------------------
    if (!message.isGroup()) {

        // 51960069146@s.whatsapp.net
        // +51960069146
        // 51960069146
        // => +51960069146

        String digits = conversationKey
                .replace("@s.whatsapp.net", "")
                .replace("@lid", "")
                .replaceAll("[^0-9]", "");

        conversationKey = "+" + digits;

    } else {
        // Los grupos conservan el JID completo
        conversationKey = conversationKey.trim();
    }

    return message.getChannel().name() + ":" + conversationKey;
}

    private Conversation resolveConversation(
            ProcessingContext context,
            String channelConversationId,
            Contact contact
    ) {

        return conversationRepository
                .findByChannelConversationIdAndTenantIdAndDeletedFalse(
                        channelConversationId,
                        context.getTenantId()
                )
                .orElseGet(() ->
                        createConversationSafely(
                                context,
                                channelConversationId,
                                contact
                        )
                );
    }

    /**
     * Evita conversaciones duplicadas en condiciones de carrera.
     */
    @Transactional
    protected Conversation createConversationSafely(
            ProcessingContext context,
            String channelConversationId,
            Contact contact
    ) {

        try {

            return createConversation(
                    context,
                    channelConversationId,
                    contact
            );

        } catch (DataIntegrityViolationException ex) {

            log.warn(
                    "Conversation already created concurrently, loading existing one: {}",
                    channelConversationId
            );

            return conversationRepository
                    .findByChannelConversationIdAndTenantIdAndDeletedFalse(
                            channelConversationId,
                            context.getTenantId()
                    )
                    .orElseThrow(() -> ex);
        }
    }

    private void updateConversationMetadata(
            Conversation conversation,
            IncomingMessage message
    ) {

        boolean changed = false;

        // ---------------------------------------------------------------------
        // Nombre del grupo
        // ---------------------------------------------------------------------
if (message.isGroup()
        && message.getConversationName() != null
        && !message.getConversationName().isBlank()
        && !"Grupo WhatsApp".equals(message.getConversationName())
        && !message.getConversationName().startsWith("Grupo ")
        && !Objects.equals(
                conversation.getSubject(),
                message.getConversationName()
        )) {

    conversation.setSubject(message.getConversationName());
    changed = true;
}

        // ---------------------------------------------------------------------
        // Tipo de conversación / instancia
        // ---------------------------------------------------------------------
        if (conversation.isGroup() != message.isGroup()) {
            conversation.setGroup(message.isGroup());
            changed = true;
        }

        if (!Objects.equals(
                conversation.getInstanceName(),
                message.getInstanceId()
        )) {

            conversation.setInstanceName(message.getInstanceId());
            changed = true;
        }

        // ---------------------------------------------------------------------
        // Actualizar última actividad
        // ---------------------------------------------------------------------
        conversation.setLastMessageAt(LocalDateTime.now(ZoneOffset.UTC));
        changed = true;

        if (changed) {
            conversationRepository.save(conversation);
        }
    }

    private Conversation createConversation(
            ProcessingContext context,
            String channelConversationId,
            Contact contact
    ) {

        IncomingMessage message = context.getIncomingMessage();

        Conversation conversation = Conversation.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .contact(contact)
                .channel(message.getChannel())
                .status(ConversationStatus.OPEN)
                .priority(ConversationPriority.MEDIUM)
                .channelConversationId(channelConversationId)

                // 👇 GRUPOS
                .isGroup(message.isGroup())
                .subject(message.getConversationName())

                .instanceName(message.getInstanceId())
                .lastMessageAt(LocalDateTime.now(ZoneOffset.UTC))
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
                message.getChannel().name(),
                contact.getFullName()
        ));

        log.info(
                "New conversation created: id={} key={} group={} subject={}",
                conversation.getId(),
                channelConversationId,
                conversation.isGroup(),
                conversation.getSubject()
        );

        return conversation;
    }
}
