package com.iquenobot.orchestrator.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

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
                .data(Map.of("pushName", "John Doe"))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(null, "OK", 100)
        );

        adapter.processWebhook("inst-abc", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());

        IncomingMessage msg = messageCaptor.getValue();
        assertEquals("msg-123", msg.getChannelMessageId());
        assertEquals(ChannelType.WHATSAPP, msg.getChannel());
        assertEquals("5511999999999", msg.getSourceIdentifier());
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
                .build();

        ProcessingResult expected = ProcessingResult.success(
                null, "Processed", 50
        );

        when(orchestrator.processMessage(any())).thenReturn(expected);

        ProcessingResult result = adapter.processWebhook("inst-abc", dto);

        assertSame(expected, result);
    }
}
