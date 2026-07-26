package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.shared.enums.ChannelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagePipelineTest {

    @Mock(lenient = true)
    private PipelineStep step1;
    @Mock(lenient = true)
    private PipelineStep step2;

    @Test
    void shouldExecuteStepsInOrder() {
        var p1 = mock(PipelineStep.class, withSettings().extraInterfaces(MessagePipeline.PrioritizedStep.class));
        var p2 = mock(PipelineStep.class, withSettings().extraInterfaces(MessagePipeline.PrioritizedStep.class));
        lenient().when(((MessagePipeline.PrioritizedStep) p1).getOrder()).thenReturn(10);
        lenient().when(((MessagePipeline.PrioritizedStep) p2).getOrder()).thenReturn(20);

        ProcessingContext ctx = new ProcessingContext();
        ctx.setIncomingMessage(IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("5511999999999")
                .build());

        when(p1.execute(ctx)).thenReturn(ctx);
        when(p2.execute(ctx)).thenReturn(ctx);

        MessagePipeline pipeline = new MessagePipeline(List.of(p2, p1));
        pipeline.execute(ctx);

        verify(p1).execute(ctx);
        verify(p2).execute(ctx);
    }

    @Test
    void shouldThrowPipelineExecutionExceptionWhenStepFails() {
        var failingStep = mock(PipelineStep.class, withSettings().extraInterfaces(MessagePipeline.PrioritizedStep.class));
        var goodStep = mock(PipelineStep.class, withSettings().extraInterfaces(MessagePipeline.PrioritizedStep.class));
        lenient().when(((MessagePipeline.PrioritizedStep) failingStep).getOrder()).thenReturn(0);
        lenient().when(((MessagePipeline.PrioritizedStep) goodStep).getOrder()).thenReturn(10);

        ProcessingContext ctx = context();
        when(failingStep.execute(ctx)).thenThrow(new RuntimeException("Step error"));

        MessagePipeline pipeline = new MessagePipeline(List.of(failingStep, goodStep));
        assertThrows(MessagePipeline.PipelineExecutionException.class, () -> pipeline.execute(ctx));
    }

    @Test
    void shouldHandleNonPrioritizedSteps() {
        var nonPrioritized = step1;
        var prioritized = mock(PipelineStep.class, withSettings().extraInterfaces(MessagePipeline.PrioritizedStep.class));
        lenient().when(((MessagePipeline.PrioritizedStep) prioritized).getOrder()).thenReturn(50);

        ProcessingContext ctx = context();
        when(nonPrioritized.execute(ctx)).thenReturn(ctx);
        when(prioritized.execute(ctx)).thenReturn(ctx);

        MessagePipeline pipeline = new MessagePipeline(List.of(prioritized, nonPrioritized));
        pipeline.execute(ctx);

        verify(nonPrioritized).execute(ctx);
        verify(prioritized).execute(ctx);
    }

    private static ProcessingContext context() {
        ProcessingContext ctx = new ProcessingContext();
        ctx.setIncomingMessage(IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("5511999999999")
                .build());
        return ctx;
    }
}
