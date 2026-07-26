package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    @Override
    public int getOrder() { return 90; }

    @Override
    public ProcessingContext execute(ProcessingContext context) {
        var msg = context.getIncomingMessage();

        log.info("AUDIT|tenantId={}|conversationId={}|contactId={}|messageId={}|channel={}|type={}",
                context.getTenantId(),
                context.getConversation() != null ? context.getConversation().getId() : null,
                context.getContact() != null ? context.getContact().getId() : null,
                msg.getChannelMessageId(),
                msg.getChannel(),
                msg.getType());

        return context;
    }
}
