package com.iquenobot.orchestrator.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

    @Mock
    private ConversationMessageRepository messageRepository;

    @InjectMocks
    private WhatsAppWebhookAdapter adapter;

    @Captor
    private ArgumentCaptor<IncomingMessage> messageCaptor;

    @Test
    void shouldConvertTextWebhookToIncomingMessage() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-123")
                .from("5511999999999")
                .type("TEXT")
                .text("Hello, bot!")
                .timestamp(LocalDateTime.of(2025, 1, 15, 10, 30))
                .data(Map.of(
                        "key", Map.of("id", "msg-123", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Hello, bot!"),
                        "pushName", "John Doe",
                        "messageTimestamp", Instant.now().plusSeconds(60).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());

        IncomingMessage msg = messageCaptor.getValue();
        assertEquals("msg-123", msg.getChannelMessageId());
        assertEquals(ChannelType.WHATSAPP, msg.getChannel());
        assertEquals("+5511999999999", msg.getSourceIdentifier());
        assertEquals("John Doe", msg.getSourceName());
        assertEquals(MessageType.TEXT, msg.getType());
        assertEquals("Hello, bot!", msg.getContent());
        assertEquals("inst-abc", msg.getInstanceId());
    }

    @Test
    void shouldConvertImageWebhook() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-456")
                .from("5511999999999")
                .type("IMAGE")
                .text(null)
                .mediaUrl("https://example.com/image.jpg")
                .caption("Check this out")
                .data(Map.of(
                        "key", Map.of("id", "msg-456", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("imageMessage", Map.of(
                                "url", "https://example.com/image.jpg",
                                "caption", "Check this out")),
                        "messageType", "imageMessage",
                        "messageTimestamp", Instant.now().plusSeconds(60).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());

        IncomingMessage msg = messageCaptor.getValue();
        assertEquals(MessageType.IMAGE, msg.getType());
        assertEquals("https://example.com/image.jpg", msg.getMediaUrl());
        assertEquals("Check this out", msg.getCaption());
    }

    @Test
    void shouldHandleUnknownTypeAsText() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-789")
                .from("5511999999999")
                .type("UNKNOWN_TYPE")
                .text("Some text")
                .data(Map.of(
                        "key", Map.of("id", "msg-789", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Some text"),
                        "messageTimestamp", Instant.now().plusSeconds(60).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());

        IncomingMessage msg = messageCaptor.getValue();
        assertEquals(MessageType.TEXT, msg.getType());
    }

    @Test
    void shouldHandleNullTypeAsText() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-000")
                .from("5511999999999")
                .type(null)
                .text("Hello")
                .data(Map.of(
                        "key", Map.of("id", "msg-000", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Hello"),
                        "messageTimestamp", Instant.now().plusSeconds(60).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());

        IncomingMessage msg = messageCaptor.getValue();
        assertEquals(MessageType.TEXT, msg.getType());
    }

    @Test
    void shouldReturnOrchestratorResult() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-111")
                .from("5511999999999")
                .type("TEXT")
                .text("Test")
                .data(Map.of(
                        "key", Map.of("id", "msg-111", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Test"),
                        "messageTimestamp", Instant.now().plusSeconds(60).getEpochSecond()
                ))
                .build();

        ProcessingResult expected = ProcessingResult.success(
                null, "Processed", 50
        );

        when(orchestrator.processMessage(any())).thenReturn(expected);

        ProcessingResult result = adapter.processWebhook("inst-abc", dto);

        assertSame(expected, result);
    }

    @Test
    void shouldProcessFreshMessageWithKeyAndTimestamp() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-fresh")
                .from("5511999999999")
                .type("TEXT")
                .text("Hola")
                .data(Map.of(
                        "key", Map.of("id", "ABC999", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Hola"),
                        "pushName", "John",
                        "messageTimestamp", Instant.now().plusSeconds(60).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        assertEquals("ABC999", messageCaptor.getValue().getChannelMessageId());
    }

    @Test
    void shouldPersistRedeliveredMessageSentLongAgo() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-pre-startup")
                .from("5511999999999")
                .type("TEXT")
                .text("Mensaje de antes del arranque")
                .data(Map.of(
                        "key", Map.of("id", "ABC111", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Mensaje de antes del arranque"),
                        "messageTimestamp", Instant.now().minusSeconds(3600).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        // Los mensajes viejos ya NO se descartan: pasan al pipeline para
        // persistirlos (sin auto-respuesta) y no perder historial.
        verify(orchestrator).processMessage(messageCaptor.capture());
        assertEquals("ABC111", messageCaptor.getValue().getChannelMessageId());
    }

    @Test
    void shouldPersistStaleRedeliveryOlderThanDeliveryWindow() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-stale")
                .from("5511999999999")
                .type("TEXT")
                .text("Mensaje viejo redelivered")
                .data(Map.of(
                        "key", Map.of("id", "ABC222", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Mensaje viejo redelivered"),
                        "messageTimestamp", Instant.now().minusSeconds(1200).getEpochSecond()
                ))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        // La decisión de NO responder en automático la toma MessagePersistenceStep;
        // el adapter deja pasar el mensaje para preservar el historial.
        verify(orchestrator).processMessage(messageCaptor.capture());
        assertEquals("ABC222", messageCaptor.getValue().getChannelMessageId());
    }

    @Test
    void shouldSkipDuplicateMessageByChannelMessageId() {
        WhatsAppWebhookDto dto = WhatsAppWebhookDto.builder()
                .messageId("msg-dup")
                .from("5511999999999")
                .type("TEXT")
                .text("Duplicado")
                .data(Map.of(
                        "key", Map.of("id", "ABC333", "remoteJid", "5511999999999@s.whatsapp.net"),
                        "message", Map.of("conversation", "Duplicado"),
                        "messageTimestamp", Instant.now().getEpochSecond()
                ))
                .build();

        when(messageRepository.existsByChannelMessageId("ABC333")).thenReturn(true);

        adapter.processWebhook("inst-abc", dto);

        verifyNoInteractions(orchestrator);
    }
}
