package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionStrategy;
import com.iquenobot.shared.enums.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AutoResponseStrategy implements DecisionStrategy {

    @Override
    public int getPriority() { return 100; }

    @Override
    public boolean canHandle(ProcessingContext context) {
        return true;
    }

    @Override
    public Decision decide(ProcessingContext context) {
        var msg = context.getIncomingMessage();

        if (msg.getType() == MessageType.SYSTEM) {
            return Decision.builder()
                    .actionType(ActionType.NO_ACTION)
                    .reason("System message, no response needed")
                    .build();
        }

        if (context.getContact() != null && !context.getContact().canReceiveMessages()) {
            return Decision.builder()
                    .actionType(ActionType.NO_ACTION)
                    .reason("Contact cannot receive messages (blocked or unsubscribed)")
                    .build();
        }

        log.debug("No specific strategy handled the message, defaulting to NO_ACTION");
        return Decision.builder()
                .actionType(ActionType.NO_ACTION)
                .reason("No automated action configured for this message")
                .build();
    }
}
