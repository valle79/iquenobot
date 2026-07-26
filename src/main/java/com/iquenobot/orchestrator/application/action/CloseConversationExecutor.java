package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.interfaces.event.ConversationClosedEvent;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloseConversationExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.CLOSE_CONVERSATION; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        var conv = context.getConversation();
        conv.markClosed();
        conversationRepository.save(conv);

        eventPublisher.publish(new ConversationClosedEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                decision.getReason(),
                "bot"
        ));

        log.info("Conversation closed: id={} reason={}", conv.getId(), decision.getReason());
    }
}
