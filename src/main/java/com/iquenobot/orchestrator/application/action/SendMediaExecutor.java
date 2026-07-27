package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.domain.service.ChannelMessageSender;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendMediaExecutor implements ActionExecutor {

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final List<ChannelMessageSender> channelSenders;

    @Override
    public ActionType supportedActionType() { return ActionType.SEND_MEDIA; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        var params = decision.getParameters();
        if (params == null) {
            log.warn("SendMediaExecutor called but no parameters provided");
            return;
        }

        String mediaUrl = (String) params.get("mediaUrl");
        String caption = (String) params.get("caption");
        String filename = (String) params.get("filename");
        MessageType mediaType = params.get("mediaType") != null
                ? MessageType.valueOf((String) params.get("mediaType"))
                : MessageType.IMAGE;

        ChannelType channel = context.getIncomingMessage().getChannel();
        ChannelMessageSender sender = channelSenders.stream()
                .filter(s -> s.supportedChannel() == channel)
                .findFirst()
                .orElse(null);

        if (sender == null) {
            log.warn("No channel sender available for channel={}", channel);
            return;
        }

        String recipient = resolveRecipient(context, channel);
        String instanceId = context.getIncomingMessage().getInstanceId();

        try {
            String messageId = sender.sendMediaMessage(instanceId, recipient, mediaUrl, caption, filename, mediaType);

            ConversationMessage botMessage = ConversationMessage.builder()
                    .id(UUID.randomUUID())
                    .tenantId(context.getTenantId())
                    .conversation(context.getConversation())
                    .direction(MessageDirection.OUTBOUND)
                    .type(mediaType)
                    .status(MessageStatus.SENT)
                    .content(caption != null ? caption : "Media message")
                    .channelMessageId(messageId)
                    .fromBot(true)
                    .sentAt(LocalDateTime.now())
                    .build();

            messageRepository.save(botMessage);

            var conv = context.getConversation();
            conv.incrementMessageCount();
            conversationRepository.save(conv);

            log.info("Media sent: type={} channel={} conversation={}", mediaType, channel, conv.getId());

        } catch (Exception e) {
            log.error("Failed to send media via {}: {}", channel, e.getMessage(), e);
        }
    }

    private String resolveRecipient(ProcessingContext context, ChannelType channel) {
        return switch (channel) {
            case WHATSAPP, SMS -> context.getContact().getPhone();
            default -> context.getIncomingMessage().getSourceIdentifier();
        };
    }
}

