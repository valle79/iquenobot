package com.iquenobot.orchestrator.application.action;

import com.iquenobot.ai.domain.dto.WhatsAppMessageDto;
import com.iquenobot.ai.domain.service.IWhatsAppProvider;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendMediaExecutor implements ActionExecutor {

    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final IWhatsAppProvider whatsAppProvider;

    @Override
    public ActionType supportedActionType() { return ActionType.SEND_MEDIA; }

    @Override
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

        WhatsAppMessageDto mediaMessage = WhatsAppMessageDto.builder()
                .to(context.getContact().getPhone())
                .type(mediaType)
                .mediaUrl(mediaUrl)
                .caption(caption)
                .filename(filename)
                .build();

        String instanceId = context.getIncomingMessage().getInstanceId();

        try {
            String messageId = whatsAppProvider.sendMediaMessage(instanceId, mediaMessage);

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

            log.info("Media sent: type={} conversation={} mediaUrl={}",
                    mediaType, conv.getId(), mediaUrl);

        } catch (Exception e) {
            log.error("Failed to send media: {}", e.getMessage(), e);
        }
    }
}
