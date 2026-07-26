package com.iquenobot.orchestrator.application.action;

import com.iquenobot.lead.domain.entity.Lead;
import com.iquenobot.lead.domain.repository.LeadRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.shared.enums.LeadSource;
import com.iquenobot.shared.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateLeadExecutor implements ActionExecutor {

    private final LeadRepository leadRepository;

    @Override
    public ActionType supportedActionType() { return ActionType.CREATE_LEAD; }

    @Override
    public void execute(Decision decision, ProcessingContext context) {
        var params = decision.getParameters();
        String channelName = context.getIncomingMessage().getChannel().name();
        String title = params != null
                ? (String) params.getOrDefault("title", "Lead from " + channelName)
                : "Lead from " + channelName;
        String description = params != null ? (String) params.get("description") : null;

        Lead lead = Lead.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .contact(context.getContact())
                .title(title)
                .description(description)
                .status(LeadStatus.NEW)
                .source(LeadSource.WHATSAPP)
                .score(0)
                .build();

        leadRepository.save(lead);
        log.info("Lead created from conversation: conversation={} leadTitle={}",
                context.getConversation().getId(), title);
    }
}
