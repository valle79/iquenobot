package com.iquenobot.orchestrator.domain.service;

import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;

/**
 * Channel-agnostic interface for sending messages through any channel.
 * Each channel (WhatsApp, Telegram, Messenger, etc.) provides its own implementation.
 */
public interface ChannelMessageSender {

    /**
     * Send a media message through the appropriate channel.
     * @param instanceId Channel-specific instance identifier
     * @param recipient Recipient identifier (phone, user ID, etc.)
     * @param mediaUrl URL of the media to send
     * @param caption Optional caption
     * @param filename Optional filename
     * @param mediaType Type of media (IMAGE, VIDEO, AUDIO, DOCUMENT)
     * @return Channel-specific message ID
     */
    String sendMediaMessage(String instanceId, String recipient, String mediaUrl,
                            String caption, String filename, MessageType mediaType);

    /**
     * Send a text message through the appropriate channel.
     * @param instanceId Channel-specific instance identifier
     * @param recipient Recipient identifier
     * @param text Message text
     * @return Channel-specific message ID
     */
    String sendTextMessage(String instanceId, String recipient, String text);

    /**
     * The channel type this sender handles.
     */
    ChannelType supportedChannel();
}
