package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.MessengerWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessengerWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

    @InjectMocks
    private MessengerWebhookAdapter adapter;

    @Captor
    private ArgumentCaptor<IncomingMessage> messageCaptor;

    @Test
    void shouldConvertTextMessage() {
        MessengerWebhookDto dto = MessengerWebhookDto.builder()
                .object("page")
                .entry(List.of(MessengerWebhookDto.Entry.builder()
                        .id("page-1")
                        .messaging(List.of(MessengerWebhookDto.Messaging.builder()
                                .sender(new MessengerWebhookDto.Sender("user-123"))
                                .recipient(new MessengerWebhookDto.Recipient("page-1"))
                                .message(MessengerWebhookDto.Message.builder()
                                        .mid("mid-123")
                                        .text("Hello from Messenger")
                                        .build())
                                .build()))
                        .build()))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(Decision.builder().actionType(ActionType.SEND_TEXT).build(), "OK", 50)
        );

        adapter.processWebhook("inst-1", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        IncomingMessage msg = messageCaptor.getValue();

        assertEquals("mid-123", msg.getChannelMessageId());
        assertEquals(ChannelType.MESSENGER, msg.getChannel());
        assertEquals("user-123", msg.getSourceIdentifier());
        assertEquals(MessageType.TEXT, msg.getType());
        assertEquals("Hello from Messenger", msg.getContent());
        assertEquals("inst-1", msg.getInstanceId());
    }

    @Test
    void shouldConvertImageAttachment() {
        MessengerWebhookDto dto = MessengerWebhookDto.builder()
                .object("page")
                .entry(List.of(MessengerWebhookDto.Entry.builder()
                        .messaging(List.of(MessengerWebhookDto.Messaging.builder()
                                .sender(new MessengerWebhookDto.Sender("user-123"))
                                .message(MessengerWebhookDto.Message.builder()
                                        .mid("mid-456")
                                        .attachments(List.of(MessengerWebhookDto.Attachment.builder()
                                                .type("image")
                                                .payload(new MessengerWebhookDto.Payload("https://example.com/img.jpg", null, null))
                                                .build()))
                                        .build())
                                .build()))
                        .build()))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(Decision.builder().actionType(ActionType.SEND_TEXT).build(), "OK", 50)
        );

        adapter.processWebhook("inst-1", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        IncomingMessage msg = messageCaptor.getValue();

        assertEquals(MessageType.IMAGE, msg.getType());
        assertEquals("https://example.com/img.jpg", msg.getMediaUrl());
    }

    @Test
    void shouldReturnFailureWhenNoEntries() {
        MessengerWebhookDto dto = MessengerWebhookDto.builder()
                .object("page")
                .entry(List.of())
                .build();

        ProcessingResult result = adapter.processWebhook("inst-1", dto);

        assertFalse(result.isSuccess());
        assertEquals("NO_ENTRIES", result.getErrorCode());
        verifyNoInteractions(orchestrator);
    }
}
