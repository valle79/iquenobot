package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.SmsWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

    @InjectMocks
    private SmsWebhookAdapter adapter;

    @Captor
    private ArgumentCaptor<IncomingMessage> messageCaptor;

    @Test
    void shouldConvertSmsMessage() {
        SmsWebhookDto dto = SmsWebhookDto.builder()
                .messageSid("SM123")
                .from("+1234567890")
                .to("+0987654321")
                .body("Hello via SMS")
                .numMedia("0")
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(Decision.builder().actionType(ActionType.SEND_TEXT).build(), "OK", 50)
        );

        adapter.processWebhook("inst-1", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        IncomingMessage msg = messageCaptor.getValue();

        assertEquals("SM123", msg.getChannelMessageId());
        assertEquals(ChannelType.SMS, msg.getChannel());
        assertEquals("+1234567890", msg.getSourceIdentifier());
        assertEquals(MessageType.TEXT, msg.getType());
        assertEquals("Hello via SMS", msg.getContent());
    }
}
