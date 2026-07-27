package com.iquenobot.orchestrator.application.action;

import com.iquenobot.lead.domain.entity.Lead;
import com.iquenobot.lead.domain.repository.LeadRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.orchestrator.interfaces.event.LeadCreatedEvent;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.LeadSource;
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
public class CreateLeadExecutor implements ActionExecutor {

    private final LeadRepository leadRepository;
    private final EventPublisher eventPublisher;

    private static final Map<ChannelType, LeadSource> CHANNEL_TO_SOURCE = Map.of(
            ChannelType.WHATSAPP, LeadSource.WHATSAPP,
            ChannelType.EMAIL, LeadSource.EMAIL,
            ChannelType.WEBCHAT, LeadSource.WEB_FORM,
            ChannelType.MESSENGER, LeadSource.SOCIAL_MEDIA,
            ChannelType.INSTAGRAM, LeadSource.SOCIAL_MEDIA,
            ChannelType.SMS, LeadSource.PHONE,
            ChannelType.TWITTER, LeadSource.SOCIAL_MEDIA,
            ChannelType.TELEGRAM, LeadSource.SOCIAL_MEDIA,
            ChannelType.API, LeadSource.DIRECT
    );

    @Override
    public ActionType supportedActionType() { return ActionType.CREATE_LEAD; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        var params = decision.getParameters();
        ChannelType channel = context.getIncomingMessage().getChannel();
        String channelName = channel.name();
        String title = params != null
                ? (String) params.getOrDefault("title", "Lead from " + channelName)
                : "Lead from " + channelName;
        String description = params != null ? (String) params.get("description") : null;
        LeadSource source = CHANNEL_TO_SOURCE.getOrDefault(channel, LeadSource.OTHER);

        Lead lead = Lead.builder()
                .tenantId(context.getTenantId())
                .contact(context.getContact())
                .title(title)
                .description(description)
                .status(LeadStatus.NEW)
                .source(source)
                .score(0)
                .build();

        leadRepository.save(lead);

        eventPublisher.publish(new LeadCreatedEvent(
                context.getTenantId().toString(),
                context.getConversation().getId().toString(),
                lead.getId().toString(),
                context.getContact().getId().toString(),
                title,
                source
        ));

        log.info("Lead created: conversation={} leadTitle={} source={}",
                context.getConversation().getId(), title, source);
    }
}
