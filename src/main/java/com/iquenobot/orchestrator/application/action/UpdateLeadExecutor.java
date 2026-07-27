package com.iquenobot.orchestrator.application.action;

import com.iquenobot.lead.domain.entity.Lead;
import com.iquenobot.lead.domain.repository.LeadRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.shared.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateLeadExecutor implements ActionExecutor {

    private final LeadRepository leadRepository;

    @Override
    public ActionType supportedActionType() { return ActionType.UPDATE_LEAD; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        Map<String, Object> params = decision.getParameters();
        if (params == null || !params.containsKey("leadId")) {
            log.warn("UpdateLeadExecutor called without leadId parameter");
            return;
        }

        UUID leadId = UUID.fromString((String) params.get("leadId"));
        Lead lead = leadRepository.findById(leadId).orElse(null);
        if (lead == null) {
            log.warn("Lead not found: {}", leadId);
            return;
        }

        if (params.containsKey("status")) {
            lead.setStatus(LeadStatus.valueOf((String) params.get("status")));
        }
        if (params.containsKey("score")) {
            lead.setScore(((Number) params.get("score")).intValue());
        }
        if (params.containsKey("description")) {
            lead.setDescription((String) params.get("description"));
        }

        leadRepository.save(lead);
        log.info("Lead updated: id={} status={}", leadId, lead.getStatus());
    }
}
