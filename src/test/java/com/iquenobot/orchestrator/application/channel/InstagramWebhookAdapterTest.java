package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.InstagramWebhookDto;
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
class InstagramWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

    @InjectMocks
    private InstagramWebhookAdapter adapter;

    @Captor
    private ArgumentCaptor<IncomingMessage> messageCaptor;

    @Test
    void shouldConvertTextMessage() {
        InstagramWebhookDto dto = InstagramWebhookDto.builder()
                .object("instagram")
                .entry(List.of(InstagramWebhookDto.Entry.builder()
                        .id("ig-page-1")
                        .messaging(List.of(InstagramWebhookDto.Messaging.builder()
                                .sender(new InstagramWebhookDto.Sender("ig-user-1"))
                                .recipient(new InstagramWebhookDto.Recipient("ig-page-1"))
                                .message(InstagramWebhookDto.Message.builder()
                                        .mid("ig-mid-1")
                                        .text("Hello from Instagram")
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

        assertEquals("ig-mid-1", msg.getChannelMessageId());
        assertEquals(ChannelType.INSTAGRAM, msg.getChannel());
        assertEquals("ig-user-1", msg.getSourceIdentifier());
        assertEquals(MessageType.TEXT, msg.getType());
        assertEquals("Hello from Instagram", msg.getContent());
    }

    @Test
    void shouldReturnFailureWhenNoEntries() {
        InstagramWebhookDto dto = InstagramWebhookDto.builder()
                .object("instagram")
                .entry(List.of())
                .build();

        ProcessingResult result = adapter.processWebhook("inst-1", dto);

        assertFalse(result.isSuccess());
        assertEquals("NO_ENTRIES", result.getErrorCode());
        verifyNoInteractions(orchestrator);
    }
}
