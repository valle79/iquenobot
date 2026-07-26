package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionEngineImplTest {

    @Mock(lenient = true)
    private DecisionStrategy lowPriorityMock;
    @Mock(lenient = true)
    private DecisionStrategy highPriorityMock;

    @Test
    void shouldSelectFirstStrategyThatCanHandle() {
        lenient().when(lowPriorityMock.getPriority()).thenReturn(10);
        lenient().when(highPriorityMock.getPriority()).thenReturn(30);

        ProcessingContext ctx = new ProcessingContext();
        Decision highDecision = Decision.builder()
                .actionType(ActionType.SEND_TEXT)
                .reason("High priority handled")
                .build();

        when(lowPriorityMock.canHandle(ctx)).thenReturn(false);
        when(highPriorityMock.canHandle(ctx)).thenReturn(true);
        when(highPriorityMock.decide(ctx)).thenReturn(highDecision);

        DecisionEngineImpl engine = new DecisionEngineImpl(List.of(highPriorityMock, lowPriorityMock));
        Decision result = engine.decide(ctx);

        assertEquals(ActionType.SEND_TEXT, result.getActionType());
        verify(lowPriorityMock).canHandle(ctx);
        verify(highPriorityMock).canHandle(ctx);
        verify(highPriorityMock).decide(ctx);
    }

    @Test
    void shouldReturnNoActionWhenNoStrategyCanHandle() {
        lenient().when(highPriorityMock.getPriority()).thenReturn(10);
        when(highPriorityMock.canHandle(any())).thenReturn(false);

        DecisionEngineImpl engine = new DecisionEngineImpl(List.of(highPriorityMock));
        Decision result = engine.decide(new ProcessingContext());

        assertEquals(ActionType.NO_ACTION, result.getActionType());
        verify(highPriorityMock).canHandle(any());
    }

    @Test
    void shouldSortStrategiesByPriority() {
        lenient().when(lowPriorityMock.getPriority()).thenReturn(100);
        lenient().when(highPriorityMock.getPriority()).thenReturn(10);

        ProcessingContext ctx = new ProcessingContext();
        Decision highDecision = Decision.builder()
                .actionType(ActionType.SEND_TEXT)
                .reason("High priority")
                .build();

        when(highPriorityMock.canHandle(ctx)).thenReturn(true);
        when(highPriorityMock.decide(ctx)).thenReturn(highDecision);

        DecisionEngineImpl engine = new DecisionEngineImpl(List.of(lowPriorityMock, highPriorityMock));
        Decision result = engine.decide(ctx);

        assertEquals(ActionType.SEND_TEXT, result.getActionType());
        verify(highPriorityMock).decide(ctx);
    }
}
