package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.EmailWebhookDto;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

    @InjectMocks
    private EmailWebhookAdapter adapter;

    @Captor
    private ArgumentCaptor<IncomingMessage> messageCaptor;

    @Test
    void shouldConvertEmailWithSubjectAndBody() {
        EmailWebhookDto dto = EmailWebhookDto.builder()
                .messageId("email-123")
                .from("sender@example.com")
                .fromName("John Doe")
                .to("support@company.com")
                .subject("Help needed")
                .bodyText("I need help with my account")
                .receivedAt(LocalDateTime.of(2025, 1, 15, 10, 0))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(Decision.builder().actionType(ActionType.SEND_TEXT).build(), "OK", 50)
        );

        adapter.processWebhook("inst-1", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        IncomingMessage msg = messageCaptor.getValue();

        assertEquals("email-123", msg.getChannelMessageId());
        assertEquals(ChannelType.EMAIL, msg.getChannel());
        assertEquals("sender@example.com", msg.getSourceIdentifier());
        assertEquals("John Doe", msg.getSourceName());
        assertEquals(MessageType.TEXT, msg.getType());
        assertTrue(msg.getContent().contains("Help needed"));
        assertTrue(msg.getContent().contains("I need help with my account"));
    }
}
