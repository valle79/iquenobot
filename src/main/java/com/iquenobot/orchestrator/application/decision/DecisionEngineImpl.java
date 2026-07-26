package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionEngine;
import com.iquenobot.orchestrator.domain.service.DecisionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@Slf4j
public class DecisionEngineImpl implements DecisionEngine {

    private final List<DecisionStrategy> strategies;

    public DecisionEngineImpl(List<DecisionStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(DecisionStrategy::getPriority))
                .toList();
        log.info("DecisionEngine initialized with {} strategies", strategies.size());
    }

    @Override
    public Decision decide(ProcessingContext context) {
        log.debug("Running decision engine with {} strategies", strategies.size());

        for (DecisionStrategy strategy : strategies) {
            if (strategy.canHandle(context)) {
                Decision decision = strategy.decide(context);
                log.debug("Strategy {} decided: action={} reason={}",
                        strategy.getClass().getSimpleName(),
                        decision.getActionType(),
                        decision.getReason());
                return decision;
            }
        }

        log.warn("No strategy could handle the context, returning default NO_ACTION");
        return Decision.builder()
                .actionType(com.iquenobot.orchestrator.domain.model.ActionType.NO_ACTION)
                .reason("No strategy could handle the message")
                .build();
    }
}
