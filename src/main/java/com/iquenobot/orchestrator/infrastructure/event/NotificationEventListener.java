package com.iquenobot.orchestrator.infrastructure.event;

import com.iquenobot.notification.application.NotificationService;
import com.iquenobot.notification.domain.dto.CreateNotificationRequestDto;
import com.iquenobot.orchestrator.interfaces.event.*;
import com.iquenobot.shared.enums.NotificationChannel;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listener that creates notifications based on Orchestrator events.
 * Sends real-time notifications to users about important events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @EventListener
    public void handleMessageReceived(MessageReceivedEvent event) {
        // Don't notify on every message received (too noisy)
        // Only notify if specific conditions are met
        log.debug("Message received event processed for notifications: conversationId={}",
                event.getConversationId());
    }

    @Async
    @EventListener
    public void handleConversationCreated(ConversationCreatedEvent event) {
        try {
            log.info("Creating notification for new conversation: conversationId={}",
                    event.getConversationId());

            // TODO: Get supervisor/admin userId from tenant configuration
            // For now, this is a placeholder - you would query the appropriate user to notify

            // Example notification creation (commented out until we have user resolution logic)
            /*
            CreateNotificationRequestDto notification = CreateNotificationRequestDto.builder()
                    .userId(supervisorUserId) // TODO: Resolve from tenant
                    .type(NotificationType.NEW_CONVERSATION)
                    .channel(NotificationChannel.IN_APP)
                    .priority(NotificationPriority.NORMAL)
                    .title("Nueva Conversación")
                    .message("Nueva conversación iniciada desde " + event.getChannel())
                    .actionUrl("/conversations/" + event.getConversationId())
                    .actionLabel("Ver Conversación")
                    .relatedEntityType("Conversation")
                    .relatedEntityId(event.getConversationId())
                    .build();

            notificationService.create(notification);
            */

        } catch (Exception e) {
            log.error("Error creating notification for conversation created event", e);
        }
    }

    @Async
    @EventListener
    public void handleAgentAssigned(AgentAssignedEvent event) {
        try {
            log.info("Creating notification for agent assignment: agentId={} conversationId={}",
                    event.getAgentId(), event.getConversationId());

            UUID agentUserId = UUID.fromString(event.getAgentId());

            CreateNotificationRequestDto notification = CreateNotificationRequestDto.builder()
                    .userId(agentUserId)
                    .type(NotificationType.CONVERSATION_ASSIGNED)
                    .channel(NotificationChannel.IN_APP)
                    .priority(NotificationPriority.HIGH)
                    .title("Conversación Asignada")
                    .message("Se te ha asignado una nueva conversación")
                    .actionUrl("/conversations/" + event.getConversationId())
                    .actionLabel("Atender Ahora")
                    .icon("assignment")
                    .relatedEntityType("Conversation")
                    .relatedEntityId(event.getConversationId())
                    .build();

            notificationService.create(notification);
            log.info("Notification created successfully for agent assignment");

        } catch (Exception e) {
            log.error("Error creating notification for agent assigned event", e);
        }
    }

    @Async
    @EventListener
    public void handleBotAnswered(BotAnsweredEvent event) {
        // Bot answers are automatic, usually don't need notifications
        // Unless the bot fails or needs human intervention
        log.debug("Bot answered event processed: conversationId={}", event.getConversationId());
    }

    @Async
    @EventListener
    public void handleConversationClosed(ConversationClosedEvent event) {
        try {
            log.info("Processing conversation closed event: conversationId={} reason={}",
                    event.getConversationId(), event.getReason());

            // Optionally notify the agent who handled the conversation
            // TODO: Resolve agent from conversation

        } catch (Exception e) {
            log.error("Error processing conversation closed event", e);
        }
    }

    @Async
    @EventListener
    public void handleActionExecuted(ActionExecutedEvent event) {
        // Most actions don't need notifications
        // But some critical ones might (e.g., CREATE_LEAD, ESCALATE_SUPERVISOR)
        log.debug("Action executed event processed: action={}", event.getActionType());

        // TODO: Add notification logic for critical actions if needed
    }
}
