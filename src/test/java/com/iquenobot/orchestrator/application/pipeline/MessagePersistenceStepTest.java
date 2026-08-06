package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.conversation.domain.repository.MessageAttachmentRepository;
import com.iquenobot.conversation.application.ConversationHandoffService;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagePersistenceStepTest {

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageAttachmentRepository attachmentRepository;

    @Mock
    private ConversationHandoffService handoffService;

    @InjectMocks
    private MessagePersistenceStep step;

    @BeforeEach
    void setUp() {
        when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldMarkConversationPendingForFreshInboundWhatsAppMessage() {
        Conversation conversation = conversation();
        ProcessingContext ctx = context(conversation, LocalDateTime.now(ZoneOffset.UTC));

        step.execute(ctx);

        assertTrue(conversation.isPendingAiResponse());
        verify(conversationRepository).save(conversation);
    }

    @Test
    void shouldPersistStaleInboundWhatsAppMessageWithoutPendingAiResponse() {
        Conversation conversation = conversation();
        ProcessingContext ctx = context(
                conversation,
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30));

        step.execute(ctx);

        assertFalse(conversation.isPendingAiResponse());
        verify(conversationRepository, never()).save(conversation);
        verify(messageRepository).save(any());
    }

    @Test
    void shouldMarkConversationPendingForStaleWebchatMessage() {
        Conversation conversation = conversation();
        ProcessingContext ctx = context(conversation, LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30));
        ctx.setIncomingMessage(IncomingMessage.builder()
                .channel(ChannelType.WEBCHAT)
                .sourceIdentifier("customer-1")
                .type(MessageType.TEXT)
                .content("Hola")
                .timestamp(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30))
                .build());

        step.execute(ctx);

        // La excepción por mensaje viejo aplica SOLO a WhatsApp; otros canales
        // conservan su comportamiento de responder normalmente.
        assertTrue(conversation.isPendingAiResponse());
        verify(conversationRepository).save(conversation);
    }

    private static Conversation conversation() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        return conversation;
    }

    private static ProcessingContext context(Conversation conversation, LocalDateTime timestamp) {
        Contact contact = new Contact();
        contact.setFullName("Juan");
        contact.setPhone("51999999999");

        ProcessingContext ctx = new ProcessingContext();
        ctx.setTenantId(UUID.randomUUID());
        ctx.setConversation(conversation);
        ctx.setContact(contact);
        ctx.setIncomingMessage(IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("51999999999")
                .type(MessageType.TEXT)
                .content("Hola")
                .timestamp(timestamp)
                .build());
        return ctx;
    }
}
