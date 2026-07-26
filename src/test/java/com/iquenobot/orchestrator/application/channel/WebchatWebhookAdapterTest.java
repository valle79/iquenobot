package com.iquenobot.orchestrator.application.channel;

import com.iquenobot.orchestrator.application.ConversationOrchestrator;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.orchestrator.interfaces.dto.WebchatWebhookDto;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebchatWebhookAdapterTest {

    @Mock
    private ConversationOrchestrator orchestrator;

    @InjectMocks
    private WebchatWebhookAdapter adapter;

    @Captor
    private ArgumentCaptor<IncomingMessage> messageCaptor;

    @Test
    void shouldConvertWebchatMessage() {
        WebchatWebhookDto dto = WebchatWebhookDto.builder()
                .sessionId("sess-123")
                .visitorId("vis-456")
                .sourceIdentifier("user@example.com")
                .sourceName("Jane")
                .message("Hello from webchat")
                .type("TEXT")
                .metadata(Map.of("page", "pricing"))
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(Decision.builder().actionType(ActionType.SEND_TEXT).build(), "OK", 50)
        );

        adapter.processWebhook("inst-1", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        IncomingMessage msg = messageCaptor.getValue();

        assertEquals("sess-123", msg.getChannelMessageId());
        assertEquals(ChannelType.WEBCHAT, msg.getChannel());
        assertEquals("user@example.com", msg.getSourceIdentifier());
        assertEquals("Jane", msg.getSourceName());
        assertEquals(MessageType.TEXT, msg.getType());
        assertEquals("Hello from webchat", msg.getContent());
        assertNotNull(msg.getMetadata());
        assertEquals("pricing", msg.getMetadata().get("page"));
    }

    @Test
    void shouldUseVisitorIdWhenNoSourceIdentifier() {
        WebchatWebhookDto dto = WebchatWebhookDto.builder()
                .sessionId("sess-789")
                .visitorId("vis-000")
                .message("Anonymous message")
                .build();

        when(orchestrator.processMessage(any())).thenReturn(
                ProcessingResult.success(Decision.builder().actionType(ActionType.SEND_TEXT).build(), "OK", 50)
        );

        adapter.processWebhook("inst-1", dto);

        verify(orchestrator).processMessage(messageCaptor.capture());
        IncomingMessage msg = messageCaptor.getValue();

        assertEquals("vis-000", msg.getSourceIdentifier());
    }
}
