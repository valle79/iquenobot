package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionDispatcherTest {

    @Test
    void shouldDispatchToCorrectExecutor() {
        ActionExecutor textExecutor = mock(ActionExecutor.class);
        when(textExecutor.supportedActionType()).thenReturn(ActionType.SEND_TEXT);

        ActionDispatcher dispatcher = new ActionDispatcher(List.of(textExecutor));

        Decision decision = Decision.builder()
                .actionType(ActionType.SEND_TEXT)
                .reason("test")
                .build();
        ProcessingContext ctx = new ProcessingContext();

        dispatcher.dispatch(decision, ctx);

        verify(textExecutor).execute(decision, ctx);
    }

    @Test
    void shouldNotFailWhenNoExecutorRegistered() {
        ActionDispatcher dispatcher = new ActionDispatcher(List.of());

        Decision decision = Decision.builder()
                .actionType(ActionType.SEND_TEXT)
                .reason("test")
                .build();

        dispatcher.dispatch(decision, new ProcessingContext());
    }

    @Test
    void shouldRegisterAllExecutors() {
        ActionExecutor text = mock(ActionExecutor.class);
        when(text.supportedActionType()).thenReturn(ActionType.SEND_TEXT);

        ActionExecutor media = mock(ActionExecutor.class);
        when(media.supportedActionType()).thenReturn(ActionType.SEND_MEDIA);

        ActionDispatcher dispatcher = new ActionDispatcher(List.of(text, media));

        assert dispatcher.getRegisteredExecutors().size() == 2;
        assert dispatcher.getRegisteredExecutors().containsKey(ActionType.SEND_TEXT);
        assert dispatcher.getRegisteredExecutors().containsKey(ActionType.SEND_MEDIA);
    }
}
