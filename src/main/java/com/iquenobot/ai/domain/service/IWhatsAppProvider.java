package com.iquenobot.ai.domain.service;

import com.iquenobot.ai.domain.dto.WhatsAppMessageDto;
import com.iquenobot.shared.enums.WhatsAppProvider;

/**
 * Interface for WhatsApp providers.
 * Implementations: Evolution API, WhatsApp Cloud API, Baileys, Twilio
 * 
 * This interface decouples the system from any specific WhatsApp provider,
 * allowing easy switching between providers without changing business logic.
 */
public interface IWhatsAppProvider {

    /**
     * Configure webhook URL for the instance.
     * Evolution API will send incoming message events to this URL.
     * @param instanceId WhatsApp instance/phone identifier
     * @param webhookUrl URL to receive webhook events
     */
    void setWebhook(String instanceId, String webhookUrl);

    /**
     * Create a new WhatsApp instance on the provider automatically.
     * @param instanceName Suggested instance name/identifier
     * @return The actual instance name created (or the same one if it already existed)
     */
    String createInstance(String instanceName);

    /**
     * Send a text message
     * @param instanceId WhatsApp instance/phone identifier
     * @param message Message details
     * @return Message ID from provider
     */
    String sendMessage(String instanceId, WhatsAppMessageDto message);

    /**
     * Send a media message (image, video, document, etc.)
     * @param instanceId WhatsApp instance/phone identifier
     * @param message Message with media details
     * @return Message ID from provider
     */
    String sendMediaMessage(String instanceId, WhatsAppMessageDto message);

    /**
     * Mark a message as read
     * @param instanceId WhatsApp instance/phone identifier
     * @param messageId Message ID to mark as read
     */
    void markAsRead(String instanceId, String messageId);

    /**
     * Get instance connection status
     * @param instanceId WhatsApp instance/phone identifier
     * @return true if connected, false otherwise
     */
    boolean isConnected(String instanceId);

    /**
     * Get QR code for connection (if applicable)
     * @param instanceId WhatsApp instance/phone identifier
     * @return Base64 encoded QR code
     */
    String getQRCode(String instanceId);

    /**
     * Disconnect instance
     * @param instanceId WhatsApp instance/phone identifier
     */
    void disconnect(String instanceId);

    /**
     * Get provider type
     * @return Provider enum value
     */
    WhatsAppProvider getProviderType();

    /**
     * Validate webhook signature (if applicable)
     * @param payload Webhook payload
     * @param signature Signature from headers
     * @return true if valid, false otherwise
     */
    boolean validateWebhookSignature(String payload, String signature);
}