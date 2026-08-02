package com.iquenobot.orchestrator.application.action;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.interfaces.event.AgentAssignedEvent;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssignAgentExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.ASSIGN_AGENT; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        Conversation conv = context.getConversation();

        if (conv.getAssignedUser() != null) {
            log.debug("Conversation {} already assigned to agent {}", conv.getId(), conv.getAssignedUser().getId());
            return;
        }

        assignIfUnassigned(conv, context.getTenantId(), context.getContact().getId());
    }

    /**
     * Asigna el agente menos ocupado a la conversación si sigue sin asignar.
     * Reutilizado por el flujo de consolidación (scheduler) cuando el
     * mensaje entrante se responde de forma diferida.
     */
    @Transactional
    public void assignIfUnassigned(Conversation conv, java.util.UUID tenantId, java.util.UUID contactId) {
        if (conv.getAssignedUser() != null) {
            log.debug("Conversation {} already assigned to agent {}", conv.getId(), conv.getAssignedUser().getId());
            return;
        }

        User agent = selectLeastBusyAgent(tenantId);

        if (agent == null) {
            log.warn("No available agents for tenant={}, conversation {} will remain unassigned",
                    tenantId, conv.getId());
            conv.setStatus(ConversationStatus.OPEN);
            conversationRepository.save(conv);
            return;
        }

        conv.assignTo(agent);
        conversationRepository.save(conv);

        eventPublisher.publish(new AgentAssignedEvent(
                tenantId.toString(),
                conv.getId().toString(),
                agent.getId().toString(),
                contactId.toString()
        ));

        log.info("Conversation {} assigned to agent {} (least busy, {} active conversations)",
                conv.getId(), agent.getId(), conversationRepository.countByAssignedUser(tenantId, agent.getId()));
    }

    private User selectLeastBusyAgent(java.util.UUID tenantId) {
        List<User> agents = userRepository.findActiveUsersByRoles(tenantId,
                List.of(RoleType.TENANT_ADMIN, RoleType.SUPERVISOR, RoleType.AGENT));

        if (agents.isEmpty()) {
            return null;
        }

        return agents.stream()
                .min(Comparator.comparingLong(agent ->
                        conversationRepository.countByAssignedUser(tenantId, agent.getId())))
                .orElse(null);
    }
}
