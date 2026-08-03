package com.iquenobot.orchestrator.application.action;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.domain.service.ChannelMessageSender;
import com.iquenobot.orchestrator.interfaces.event.AgentAssignedEvent;
import com.iquenobot.orchestrator.domain.service.EventPublisher;
import com.iquenobot.shared.application.MessageTemplateResolver;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.SenderType;
import com.iquenobot.shared.enums.ChannelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final ConversationMessageRepository messageRepository;
    private final List<ChannelMessageSender> channelSenders;
    private final MessageTemplateResolver templateResolver;

    @Override
    public ActionType supportedActionType() { return ActionType.TRANSFER_CONVERSATION; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        sendHandoffMessage(decision, context);

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

private void sendHandoffMessage(Decision decision, ProcessingContext context) {

    String responseText = decision.getParameters() != null
            ? (String) decision.getParameters().get("response")
            : null;

    if (responseText == null || responseText.isBlank()) {
        return;
    }

    responseText = templateResolver.resolve(
            responseText,
            context.getTenant(),
            context.getContact());

    String channelMessageId = null;

    try {
        var channel = context.getIncomingMessage().getChannel();

        ChannelMessageSender sender = channelSenders.stream()
                .filter(s -> s.supportedChannel() == channel)
                .findFirst()
                .orElse(null);

        if (sender != null) {

            String instanceId = context.getIncomingMessage().getInstanceId();

            Conversation conversation = context.getConversation();

            String recipient;

            // ===== WHATSAPP GRUPOS =====
            if (conversation != null
                    && conversation.isGroup()
                    && channel == ChannelType.WHATSAPP) {

                recipient = conversation.resolveChannelRecipient();

            } else {

                // ===== CHAT DIRECTO =====
                recipient = switch (channel) {

                    case WHATSAPP, SMS -> {

                        String number = context.getContact().resolveWhatsAppNumber();

                        yield number != null
                                ? number.replace("+", "")
                                : null;
                    }

                    default -> context.getIncomingMessage().getSourceIdentifier();
                };
            }

            if (recipient == null || recipient.isBlank()) {
                throw new IllegalStateException(
                        "No se pudo resolver el destinatario del mensaje de transferencia");
            }

            channelMessageId = sender.sendTextMessage(
                    instanceId,
                    recipient,
                    responseText);

            log.info(
                    "Handoff message sent via {}: conversation={} recipient={}",
                    channel,
                    context.getConversation().getId(),
                    recipient);

        } else {

            log.warn(
                    "No channel sender available for channel={}, handoff message not delivered",
                    channel);
        }

    } catch (Exception e) {

        log.error(
                "Failed to send handoff message via channel: {}",
                e.getMessage(),
                e);
    }

    try {
        ConversationMessage botMessage = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(context.getTenantId())
                .conversation(context.getConversation())
                .direction(MessageDirection.OUTBOUND)
                .senderType(SenderType.BOT)
                .type(MessageType.TEXT)
                .status(channelMessageId != null
                        ? MessageStatus.SENT
                        : MessageStatus.FAILED)
                .content(responseText)
                .channelMessageId(channelMessageId)
                .fromBot(true)
                .botIntent("handoff")
                .sentAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        messageRepository.save(botMessage);

    } catch (Exception e) {

        log.error(
                "Failed to persist handoff message: {}",
                e.getMessage(),
                e);
    }
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
