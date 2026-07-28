package com.iquenobot.ai.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ContactStatus;
import com.iquenobot.shared.enums.ConversationPriority;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private final SettingRepository settingRepository;
    private final ContactRepository contactRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ChatbotService chatbotService;

    private static final String WHATSAPP_CATEGORY = "whatsapp";
    private static final String INSTANCE_ID_KEY = "instance_id";

    private final Map<String, UUID> instanceCache = new ConcurrentHashMap<>();

    @Transactional
    public void processIncomingMessage(String instanceId, WhatsAppWebhookDto payload) {

        UUID tenantId = findTenantByInstanceId(instanceId);

        if (tenantId == null) {
            log.warn("No tenant found for WhatsApp instance: {}", instanceId);
            return;
        }

        try {
            TenantContext.setTenantId(tenantId.toString());

            String senderPhone = payload.getFrom();

            if (senderPhone == null || senderPhone.isBlank()) {
                log.warn("Incoming WhatsApp message without sender phone. Payload: {}", payload);
                return;
            }

            String senderName = extractSenderName(payload);

            Contact contact = findOrCreateContact(tenantId, senderPhone, senderName);

            Conversation conversation = findOrCreateConversation(
                    tenantId,
                    contact,
                    instanceId
            );

            createIncomingMessage(conversation, contact, payload);

            triggerChatbot(tenantId, conversation, contact, payload);

        } finally {
            TenantContext.clear();
        }
    }

    // -------------------------------------------------------------------------
    // TENANT RESOLUTION
    // -------------------------------------------------------------------------

    private UUID findTenantByInstanceId(String instanceId) {

        return instanceCache.computeIfAbsent(instanceId, id ->
                settingRepository
                        .findByCategoryAndKeyAndValueAndDeletedFalse(
                                WHATSAPP_CATEGORY,
                                INSTANCE_ID_KEY,
                                id
                        )
                        .map(Setting::getTenantId)
                        .orElse(null)
        );
    }

    public void invalidateInstanceCache(String instanceId) {
        instanceCache.remove(instanceId);
    }

    public void evictAllCachedInstances() {
        instanceCache.clear();
    }

    // -------------------------------------------------------------------------
    // CONTACT
    // -------------------------------------------------------------------------

    private Contact findOrCreateContact(UUID tenantId, String phone, String name) {

        return contactRepository
                .findByPhoneAndTenantIdAndDeletedFalse(phone, tenantId)
                .orElseGet(() -> {

                    Contact newContact = Contact.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .phone(phone)
                            .whatsappPhone(phone)
                            .fullName(name != null && !name.isBlank()
                                    ? name
                                    : phone)
                            .status(ContactStatus.ACTIVE)
                            .conversationCount(0)
                            .messageCount(0)
                            .subscribed(true)
                            .build();

                    Contact saved = contactRepository.save(newContact);

                    log.info("New WhatsApp contact created: {} ({})",
                            saved.getFullName(),
                            saved.getPhone());

                    return saved;
                });
    }

    // -------------------------------------------------------------------------
    // CONVERSATION
    // -------------------------------------------------------------------------

    private Conversation findOrCreateConversation(
            UUID tenantId,
            Contact contact,
            String instanceId
    ) {

        String channelConversationId = instanceId + ":" + contact.getPhone();

        return conversationRepository
                .findByChannelConversationIdAndTenantIdAndDeletedFalse(
                        channelConversationId,
                        tenantId
                )
                .orElseGet(() -> {

                    Conversation newConversation = Conversation.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .contact(contact)
                            .channel(ChannelType.WHATSAPP)
                            .status(ConversationStatus.OPEN)
                            .priority(ConversationPriority.MEDIUM)
                            .channelConversationId(channelConversationId)
                            .lastMessageAt(LocalDateTime.now())
                            .messageCount(0)
                            .unreadCount(0)
                            .botConversation(false)
                            .build();

                    Conversation saved = conversationRepository.save(newConversation);

                    log.info("New WhatsApp conversation created: {}", saved.getId());

                    return saved;
                });
    }

    // -------------------------------------------------------------------------
    // MESSAGE
    // -------------------------------------------------------------------------

    private void createIncomingMessage(
            Conversation conversation,
            Contact contact,
            WhatsAppWebhookDto payload
    ) {

        // Evitar duplicados por channelMessageId
        if (payload.getMessageId() != null
                && messageRepository.existsByChannelMessageId(payload.getMessageId())) {

            log.debug("Duplicate WhatsApp message ignored: {}", payload.getMessageId());
            return;
        }

        ConversationMessage message = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .tenantId(conversation.getTenantId())
                .conversation(conversation)

                // 👇 IMPORTANTE
                .direction(MessageDirection.INBOUND)
                .senderType(SenderType.CUSTOMER)

                .type(resolveMessageType(payload.getType()))
                .status(MessageStatus.SENT)

                .content(payload.getText() != null ? payload.getText() : "")

                .channelMessageId(payload.getMessageId())

                .senderName(contact.getFullName())
                .senderPhone(contact.getPhone())

                .fromBot(false)

                .sentAt(payload.getTimestamp() != null
                        ? payload.getTimestamp()
                        : LocalDateTime.now())

                .build();

        messageRepository.save(message);

        // ---------------------------------------------------------------------
        // Conversation metrics
        // ---------------------------------------------------------------------

        conversation.incrementMessageCount();
        conversation.incrementUnreadCount();
        conversation.setLastMessageAt(LocalDateTime.now());

        conversationRepository.save(conversation);

        // ---------------------------------------------------------------------
        // Contact metrics
        // ---------------------------------------------------------------------

        contact.incrementMessageCount();
        contact.setLastContactedAt(LocalDateTime.now());

        contactRepository.save(contact);

        log.info(
                "Incoming WhatsApp message processed: messageId={} phone={} conversation={}",
                payload.getMessageId(),
                contact.getPhone(),
                conversation.getId()
        );
    }

    // -------------------------------------------------------------------------
    // CHATBOT
    // -------------------------------------------------------------------------

    private void triggerChatbot(
            UUID tenantId,
            Conversation conversation,
            Contact contact,
            WhatsAppWebhookDto payload
    ) {

        if (payload.getText() == null || payload.getText().isBlank()) {
            return;
        }

        try {

            chatbotService.processMessage(
                    payload.getText(),
                    Map.of(
                            "conversationId", conversation.getId().toString(),
                            "tenantId", tenantId.toString(),
                            "contactId", contact.getId().toString(),
                            "channel", "WHATSAPP"
                    )
            );

        } catch (Exception e) {

            log.warn(
                    "Chatbot processing failed for message {}: {}",
                    payload.getMessageId(),
                    e.getMessage(),
                    e
            );
        }
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String extractSenderName(WhatsAppWebhookDto payload) {

        if (payload.getData() == null) {
            return null;
        }

        Object pushName = payload.getData().get("pushName");

        return pushName instanceof String ? (String) pushName : null;
    }

    private MessageType resolveMessageType(String type) {

        if (type == null || type.isBlank()) {
            return MessageType.TEXT;
        }

        return switch (type.toUpperCase()) {

            case "IMAGE", "IMAGEMESSAGE" -> MessageType.IMAGE;

            case "VIDEO", "VIDEOMESSAGE" -> MessageType.VIDEO;

            case "AUDIO", "AUDIOMESSAGE" -> MessageType.AUDIO;

            case "DOCUMENT", "DOCUMENTMESSAGE" -> MessageType.DOCUMENT;

            case "LOCATION", "LOCATIONMESSAGE" -> MessageType.LOCATION;

            case "CONTACT", "CONTACTMESSAGE" -> MessageType.CONTACT;

            case "STICKER", "STICKERMESSAGE" -> MessageType.STICKER;

            default -> MessageType.TEXT;
        };
    }
}
