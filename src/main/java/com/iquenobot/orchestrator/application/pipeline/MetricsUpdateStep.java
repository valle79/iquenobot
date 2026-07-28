package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsUpdateStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ContactRepository contactRepository;

    @Override
    public int getOrder() { return 50; }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {
        Contact contact = context.getContact();
        Conversation conversation = context.getConversation();

        contactRepository.incrementMessageCount(contact.getId());

        log.debug("Metrics updated: contact={} conversation={}",
                contact.getId(), conversation.getId());
        return context;
    }
}
