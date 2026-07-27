package com.iquenobot.orchestrator.infrastructure.channel;

import com.iquenobot.ai.domain.dto.WhatsAppMessageDto;
import com.iquenobot.ai.domain.service.IWhatsAppProvider;
import com.iquenobot.orchestrator.domain.service.ChannelMessageSender;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppChannelMessageSender implements ChannelMessageSender {

    private final IWhatsAppProvider whatsAppProvider;

    @Override
    public ChannelType supportedChannel() { return ChannelType.WHATSAPP; }

    @Override
    public String sendMediaMessage(String instanceId, String recipient, String mediaUrl,
                                   String caption, String filename, MessageType mediaType) {
        WhatsAppMessageDto message = WhatsAppMessageDto.builder()
                .to(recipient)
                .type(mediaType)
                .mediaUrl(mediaUrl)
                .caption(caption)
                .filename(filename)
                .build();

        return whatsAppProvider.sendMediaMessage(instanceId, message);
    }

    @Override
    public String sendTextMessage(String instanceId, String recipient, String text) {
        WhatsAppMessageDto message = WhatsAppMessageDto.builder()
                .to(recipient)
                .type(MessageType.TEXT)
                .text(text)
                .build();

        return whatsAppProvider.sendMessage(instanceId, message);
    }
}
