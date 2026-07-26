package com.iquenobot.orchestrator.application;

import com.iquenobot.orchestrator.application.action.ActionDispatcher;
import com.iquenobot.orchestrator.application.pipeline.MessagePipeline;
import com.iquenobot.orchestrator.domain.model.*;
import com.iquenobot.orchestrator.domain.service.DecisionEngine;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.MessageReceivedEvent;
import com.iquenobot.shared.enums.ChannelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationOrchestratorTest {

    @Mock
    private MessagePipeline pipeline;
    @Mock
    private DecisionEngine decisionEngine;
    @Mock
    private ActionDispatcher actionDispatcher;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ConversationOrchestrator orchestrator;

    @Test
    void processMessage_shouldSucceed_whenPipelineAndDecisionSucceed() {
        IncomingMessage message = IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("5511999999999")
                .content("Hello")
                .build();

        ProcessingContext context = new ProcessingContext();
        context.setIncomingMessage(message);

        when(pipeline.execute(any())).thenReturn(context);

        Decision decision = Decision.builder()
                .actionType(ActionType.SEND_TEXT)
                .reason("Bot response")
                .build();

        when(decisionEngine.decide(context)).thenReturn(decision);

        ProcessingResult result = orchestrator.processMessage(message);

        assertTrue(result.isSuccess());
        assertEquals(ActionType.SEND_TEXT, result.getDecision().getActionType());

        verify(pipeline).execute(any());
        verify(decisionEngine).decide(context);
        verify(actionDispatcher).dispatch(decision, context);
        verify(eventPublisher).publish(any(MessageReceivedEvent.class));
    }

    @Test
    void processMessage_shouldReturnFailure_whenPipelineFails() {
        IncomingMessage message = IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("5511999999999")
                .content("Hello")
                .build();

        when(pipeline.execute(any())).thenThrow(
                new MessagePipeline.PipelineExecutionException(
                        "Step Validation failed", new IllegalArgumentException("Invalid channel"))
        );

        ProcessingResult result = orchestrator.processMessage(message);

        assertFalse(result.isSuccess());
        assertEquals("PROCESSING_ERROR", result.getErrorCode());
        assertTrue(result.getProcessingTimeMs() >= 0);
    }
}
