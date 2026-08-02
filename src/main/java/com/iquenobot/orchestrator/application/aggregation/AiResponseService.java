package com.iquenobot.orchestrator.application.aggregation;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.conversation.application.ConversationService;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.orchestrator.application.action.ActionDispatcher;
import com.iquenobot.orchestrator.application.pipeline.BotConfigurationResolutionStep;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.DecisionEngine;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Genera la respuesta automática consolidada de una conversación.
 *
 * Reutiliza el motor de decisión y el despachador de acciones existentes:
 * se construye un {@link ProcessingContext} con el texto consolidado como
 * mensaje sintético y se ejecuta el mismo flujo que el pipeline síncrono
 * (DecisionEngine + ActionDispatcher + BotDecisionStrategy), sin duplicar
 * lógica de negocio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiResponseService {

    private final DecisionEngine decisionEngine;
    private final ActionDispatcher actionDispatcher;
    private final BotConfigurationResolutionStep botConfigurationResolutionStep;
    private final TenantRepository tenantRepository;
    private final ConversationService conversationService;

    /**
     * Ejecuta la respuesta automática para el texto consolidado.
     * Debe invocarse dentro de la misma transacción que marca los mensajes
     * como procesados (ver {@link MessageAggregationService}).
     */
    @Transactional
    public void generateAndSendResponse(UUID tenantId, Conversation conversation, String consolidatedMessage) {
        TenantContext.setTenantId(tenantId.toString());

        var contact = conversation.getContact();
        if (contact == null) {
            log.warn("Conversation {} has no contact; skipping AI response", conversation.getId());
            return;
        }

        String instanceId = conversation.getInstanceName();
        if (conversation.getChannel() == ChannelType.WHATSAPP) {
            instanceId = conversationService.resolveWhatsAppInstanceId(conversation, tenantId);
        }

        // Mensaje sintético con el contenido consolidado
        IncomingMessage synthetic = IncomingMessage.builder()
                .channel(conversation.getChannel())
                .channelConversationId(conversation.getChannelConversationId())
                .sourceIdentifier(resolveSourceIdentifier(conversation, contact))
                .sourceName(contact.getFullName())
                .type(MessageType.TEXT)
                .content(consolidatedMessage)
                .instanceId(instanceId)
                .outbound(false)
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        ProcessingContext context = new ProcessingContext();
        context.setTenantId(tenantId);
        context.setTenant(loadTenant(tenantId));
        context.setConversation(conversation);
        context.setContact(contact);
        context.setIncomingMessage(synthetic);
        context.setScheduledProcessing(true);

        botConfigurationResolutionStep.execute(context);

        Decision decision = decisionEngine.decide(context);
        actionDispatcher.dispatch(decision, context);

        for (Decision secondary : context.getSecondaryDecisions()) {
            actionDispatcher.dispatch(secondary, context);
        }

        log.info("Consolidated AI response processed: conversation={} decision={}",
                conversation.getId(), decision.getActionType());
    }

    private Tenant loadTenant(UUID tenantId) {
        return tenantRepository.findByIdAndDeletedFalse(tenantId).orElse(null);
    }

    private String resolveSourceIdentifier(Conversation conversation, com.iquenobot.contact.domain.entity.Contact contact) {
        if (contact.getPhone() != null && !contact.getPhone().isBlank()) {
            return contact.getPhone();
        }
        if (contact.getNormalizedPhone() != null && !contact.getNormalizedPhone().isBlank()) {
            return contact.getNormalizedPhone();
        }
        if (contact.getEmail() != null && !contact.getEmail().isBlank()) {
            return contact.getEmail();
        }
        return conversation.getChannelConversationId() != null
                ? conversation.getChannelConversationId()
                : "unknown";
    }
}
