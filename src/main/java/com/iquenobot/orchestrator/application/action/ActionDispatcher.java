package com.iquenobot.orchestrator.application.action;

import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ActionDispatcher {

    private final Map<ActionType, ActionExecutor> executorMap;

    public ActionDispatcher(List<ActionExecutor> executors) {
        this.executorMap = executors.stream()
                .collect(Collectors.toMap(
                        ActionExecutor::supportedActionType,
                        Function.identity()
                ));
        log.info("ActionDispatcher initialized with {} executors", executors.size());
    }

    public void dispatch(Decision decision, ProcessingContext context) {
        ActionExecutor executor = executorMap.get(decision.getActionType());

        if (executor == null) {
            log.warn("No executor registered for action type: {}. Available: {}",
                    decision.getActionType(), executorMap.keySet());
            return;
        }

        log.debug("Dispatching action: {} to executor: {}",
                decision.getActionType(), executor.getClass().getSimpleName());
        executor.execute(decision, context);
    }

    public Map<ActionType, ActionExecutor> getRegisteredExecutors() {
        return executorMap;
    }
}
