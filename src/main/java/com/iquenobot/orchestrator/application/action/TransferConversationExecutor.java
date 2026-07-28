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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransferConversationExecutor implements ActionExecutor {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Override
    public ActionType supportedActionType() { return ActionType.TRANSFER_CONVERSATION; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        var conv = conversationRepository.findById(context.getConversation().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Conversation not found: " + context.getConversation().getId()));

        conv.handoffFromBot();

        User agent = selectLeastBusyAgent(context.getTenantId());

        if (agent != null) {
            conv.assignTo(agent);
        }

        eventPublisher.publish(new AgentAssignedEvent(
                context.getTenantId().toString(),
                conv.getId().toString(),
                agent != null ? agent.getId().toString() : null,
                context.getContact().getId().toString()
        ));

        log.info("Conversation {} transferred from bot to {}",
                conv.getId(), agent != null ? "agent " + agent.getId() : "queue (no agents available)");
    }

    private User selectLeastBusyAgent(UUID tenantId) {
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
