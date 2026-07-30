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

import java.time.LocalDateTime;
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

    private static final int DEDUP_HOURS = 24;

    @Override
    public ActionType supportedActionType() { return ActionType.CREATE_LEAD; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        var params = decision.getParameters();

        String intent = params != null ? (String) params.get("intent") : null;
        String title = params != null ? (String) params.get("title") : null;
        String baseScoreStr = params != null ? (String) params.get("baseScore") : null;
        String messageContent = params != null ? (String) params.get("messageContent") : "";

        ChannelType channel = context.getIncomingMessage().getChannel();
        LeadSource source = CHANNEL_TO_SOURCE.getOrDefault(channel, LeadSource.OTHER);
        String channelName = channel.name();

        if (title == null) {
            title = "Lead from " + channelName;
        }

        boolean exists = leadRepository.existsByTenantIdAndContactIdAndCreatedAtAfter(
                context.getTenantId(),
                context.getContact().getId(),
                LocalDateTime.now().minusHours(DEDUP_HOURS)
        );

        if (exists) {
            log.info("Skipping lead creation: recent lead exists for contact={} within {}h",
                    context.getContact().getId(), DEDUP_HOURS);
            return;
        }

        int baseScore = 0;
        if (baseScoreStr != null) {
            try { baseScore = Integer.parseInt(baseScoreStr); } catch (NumberFormatException ignored) {}
        }

        int score = calculateScore(baseScore, messageContent);

        StringBuilder description = new StringBuilder();
        if (intent != null) {
            description.append("Intención: ").append(intent).append("\n");
        }
        description.append("Origen: ").append(source).append("\n");
        description.append("Score: ").append(score).append("/100");
        if (messageContent != null && !messageContent.isBlank()) {
            description.append("\nMensaje: ").append(messageContent.length() > 200
                    ? messageContent.substring(0, 200) : messageContent);
        }

        Lead lead = Lead.builder()
                .tenantId(context.getTenantId())
                .contact(context.getContact())
                .title(title)
                .description(description.toString())
                .status(LeadStatus.NEW)
                .source(source)
                .score(score)
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

        log.info("Lead created: conversation={} title={} score={} source={}",
                context.getConversation().getId(), title, score, source);
    }

    private int calculateScore(int baseScore, String message) {
        if (message == null) message = "";

        int bonus = 0;
        String lower = message.toLowerCase();

        if (lower.contains("urgente")) bonus += 20;
        if (lower.contains("hoy")) bonus += 10;
        if (lower.contains("comprar") || lower.contains("compro")) bonus += 15;
        if (lower.contains("contratar") || lower.contains("contrato")) bonus += 15;
        if (lower.contains("precio") || lower.contains("cuánto") || lower.contains("cuanto")) bonus += 10;
        if (lower.contains("ahora") || lower.contains("ya")) bonus += 10;
        if (lower.contains("cotización") || lower.contains("cotizacion") || lower.contains("presupuesto")) bonus += 15;
        if (lower.length() > 100) bonus += 5;

        return Math.min(baseScore + bonus, 100);
    }
}
