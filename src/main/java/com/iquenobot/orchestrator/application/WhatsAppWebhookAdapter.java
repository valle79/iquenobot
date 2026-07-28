package com.iquenobot.orchestrator.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookAdapter {

    private final ConversationOrchestrator orchestrator;
    private final ConversationMessageRepository messageRepository;

    public ProcessingResult processWebhook(String instanceId, WhatsAppWebhookDto payload) {

        log.info("Processing Evolution webhook for instance={}", instanceId);

        if (payload == null) {
            log.warn("Webhook payload is null, skipping");
            return ProcessingResult.empty();
        }

        Map<String, Object> data = payload.getData();

        if (data == null) {
            log.warn("Webhook data is null, skipping");
            return ProcessingResult.empty();
        }

        log.debug("Evolution webhook data={}", data);

        // ---------------------------------------------------------------------
        // DEDUP: evitar procesar mensajes ya recibidos
        // ---------------------------------------------------------------------
        String messageId = extractMessageId(data);
        if (messageId != null && messageRepository.existsByChannelMessageId(messageId)) {
            log.info("Duplicate WhatsApp message skipped: channelMessageId={}", messageId);
            return ProcessingResult.empty();
        }

        // ---------------------------------------------------------------------
        // VALIDACIÓN DE TELÉFONO
        // ---------------------------------------------------------------------
        String senderPhone = extractRemoteJid(data);
        if (senderPhone == null || senderPhone.isBlank()) {
            log.warn("WhatsApp message without sender phone, skipping");
            return ProcessingResult.empty();
        }

        log.info("New WhatsApp message from {} | type={} | msgId={}",
                senderPhone, extractMessageType(data), messageId);

        // ---------------------------------------------------------------------
        // DIRECCIÓN: INBOUND / OUTBOUND
        // ---------------------------------------------------------------------
        boolean outbound = isFromMe(data);

        log.info("WhatsApp direction: {}", outbound ? "OUTBOUND" : "INBOUND");

        IncomingMessage message = convertToIncomingMessage(instanceId, payload, outbound);

        log.info("Incoming WhatsApp message processed: from={} direction={} type={} content={}",
                message.getSourceIdentifier(),
                outbound ? "OUTBOUND" : "INBOUND",
                message.getType(),
                message.getContent());

        return orchestrator.processMessage(message);
    }

    // -------------------------------------------------------------------------
    // FROM ME
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private boolean isFromMe(Map<String, Object> data) {

        if (data == null) {
            return false;
        }

        if (data.get("key") instanceof Map<?, ?> key) {
            return Boolean.TRUE.equals(key.get("fromMe"));
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // GROUP DETECTION
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private boolean isGroupConversation(Map<String, Object> data) {
        if (data == null) return false;
        if (data.get("key") instanceof Map<?, ?> key) {
            Object remoteJid = key.get("remoteJid");
            return remoteJid instanceof String r && r.contains("@g.us");
        }
        return false;
    }

    private String extractGroupName(Map<String, Object> data) {
        return "Grupo WhatsApp";
    }

    // -------------------------------------------------------------------------
    // CONVERSION
    // -------------------------------------------------------------------------

    private IncomingMessage convertToIncomingMessage(
            String instanceId,
            WhatsAppWebhookDto payload,
            boolean outbound
    ) {

        Map<String, Object> data = payload.getData();

        String conversationJid = extractConversationJid(data);
        String senderJid = extractRemoteJid(data);

        if (senderJid == null || senderJid.isBlank()) {
            throw new IllegalArgumentException(
                    "Unable to extract WhatsApp source identifier from webhook payload"
            );
        }

        String messageId = extractMessageId(data);
        String pushName = extractPushName(data);
        String messageTypeStr = extractMessageType(data);
        String text = extractText(data);
        Long timestamp = extractTimestamp(data);

        MessageType type = resolveMessageType(messageTypeStr);

        boolean isGroup = isGroupConversation(data);
        String groupName = extractGroupName(data);

        return IncomingMessage.builder()
                .channelMessageId(messageId)
                .channel(ChannelType.WHATSAPP)
                .channelConversationId(conversationJid != null ? cleanJid(conversationJid) : null)
                .sourceIdentifier(senderJid)
                .sourceName(pushName != null && !pushName.isBlank()
                        ? pushName
                        : senderJid)
                .type(type)
                .content(text != null ? text : "")
                .instanceId(instanceId)
                .conversationName(isGroup ? groupName : null)
                .metadata(data)
                .outbound(outbound)
                .timestamp(timestamp != null
                        ? LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
                        : LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // CONVERSATION JID (remoteJid: grupo @g.us o individual @s.whatsapp.net)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String extractConversationJid(Map<String, Object> data) {

        if (data == null) {
            return null;
        }

        if (data.get("key") instanceof Map<?, ?> key) {
            Object remoteJid = key.get("remoteJid");
            if (remoteJid instanceof String r && !r.isBlank()) {
                return r.trim();
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // REMOTE JID
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
private String extractRemoteJid(Map<String, Object> data) {

    if (data == null) {
        return null;
    }

    if (data.get("key") instanceof Map<?, ?> key) {

        // -------------------------------------------------------------
        // 🔥 PRIORIDAD 1: remoteJidAlt (número real del contacto)
        // -------------------------------------------------------------
        Object remoteJidAlt = key.get("remoteJidAlt");
        if (remoteJidAlt instanceof String alt && !alt.isBlank()) {
            String normalized = normalizePhone(cleanJid(alt));

            log.debug("Resolved WhatsApp phone from remoteJidAlt: {} -> {}",
                    alt, normalized);

            return normalized;
        }

        // -------------------------------------------------------------
        // PRIORIDAD 2: participant
        // -------------------------------------------------------------
        Object participant = key.get("participant");
        if (participant instanceof String p && !p.isBlank()) {
            String normalized = normalizePhone(cleanJid(p));

            log.debug("Resolved WhatsApp phone from participant: {} -> {}",
                    p, normalized);

            return normalized;
        }

        // -------------------------------------------------------------
        // PRIORIDAD 3: remoteJid
        // -------------------------------------------------------------
        Object remoteJid = key.get("remoteJid");
        if (remoteJid instanceof String r && !r.isBlank()) {
            String normalized = normalizePhone(cleanJid(r));

            log.debug("Resolved WhatsApp phone from remoteJid: {} -> {}",
                    r, normalized);

            return normalized;
        }
    }

    // -------------------------------------------------------------
    // Fallback Evolution API
    // -------------------------------------------------------------
    Object source = data.get("source");
    if (source instanceof String s && !s.isBlank()) {
        String normalized = normalizePhone(cleanJid(s));

        log.debug("Resolved WhatsApp phone from source: {} -> {}",
                s, normalized);

        return normalized;
    }

    log.warn("Unable to resolve WhatsApp phone from webhook payload: keys={}", data.keySet());

    return null;
}

private String cleanJid(String jid) {

    if (jid == null) {
        return null;
    }

    return jid
            .replace("@s.whatsapp.net", "")
            .replace("@g.us", "")
            .replace("@lid", "")
            .trim();
}

    private String normalizePhone(String value) {

    if (value == null || value.isBlank()) {
        return null;
    }

    String normalized = value
            .replaceAll("\\\\s+", "")
            .trim();

    // Si viene como 51945756189
    if (normalized.matches("^51\\\\d{9}$")) {
        return "+" + normalized;
    }

    // Si viene como 945756189
    if (normalized.matches("^9\\\\d{8}$")) {
        return "+51" + normalized;
    }

    return normalized;
}

    // -------------------------------------------------------------------------
    // MESSAGE ID
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String extractMessageId(Map<String, Object> data) {

        if (data == null) {
            return null;
        }

        if (data.get("key") instanceof Map<?, ?> key) {
            Object id = key.get("id");
            return id instanceof String ? (String) id : null;
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // PUSH NAME
    // -------------------------------------------------------------------------

    private String extractPushName(Map<String, Object> data) {

        if (data == null) {
            return null;
        }

        Object pushName = data.get("pushName");
        return pushName instanceof String ? (String) pushName : null;
    }

    // -------------------------------------------------------------------------
    // MESSAGE TYPE
    // -------------------------------------------------------------------------

    private String extractMessageType(Map<String, Object> data) {

        if (data == null) {
            return null;
        }

        Object type = data.get("messageType");
        return type instanceof String ? (String) type : null;
    }

    // -------------------------------------------------------------------------
    // TEXT EXTRACTION
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> data) {

        if (data == null) {
            return null;
        }

        if (data.get("message") instanceof Map<?, ?> message) {

            // Texto simple
            Object conversation = message.get("conversation");
            if (conversation instanceof String c) {
                return c;
            }

            // Texto extendido
            if (message.get("extendedTextMessage") instanceof Map<?, ?> ext) {
                Object text = ext.get("text");
                if (text instanceof String t) {
                    return t;
                }
            }

            // Imagen
            if (message.get("imageMessage") instanceof Map<?, ?> img) {
                Object caption = img.get("caption");
                if (caption instanceof String c) {
                    return c;
                }
            }

            // Video
            if (message.get("videoMessage") instanceof Map<?, ?> vid) {
                Object caption = vid.get("caption");
                if (caption instanceof String c) {
                    return c;
                }
            }

            // Documento
            if (message.get("documentMessage") instanceof Map<?, ?> doc) {
                Object caption = doc.get("caption");
                if (caption instanceof String c) {
                    return c;
                }
            }

            // Audio
            if (message.get("audioMessage") instanceof Map<?, ?>) {
                return "[AUDIO]";
            }

            // Sticker
            if (message.get("stickerMessage") instanceof Map<?, ?>) {
                return "[STICKER]";
            }

            // Ubicación
            if (message.get("locationMessage") instanceof Map<?, ?>) {
                return "[LOCATION]";
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // TIMESTAMP
    // -------------------------------------------------------------------------

    private Long extractTimestamp(Map<String, Object> data) {

        if (data == null) {
            return null;
        }

        Object ts = data.get("messageTimestamp");

        if (ts instanceof Number n) {
            return n.longValue();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // MESSAGE TYPE RESOLUTION
    // -------------------------------------------------------------------------

    private MessageType resolveMessageType(String messageTypeStr) {

        if (messageTypeStr == null || messageTypeStr.isBlank()) {
            return MessageType.TEXT;
        }

        return switch (messageTypeStr.toLowerCase()) {
            case "imagemessage" -> MessageType.IMAGE;
            case "videomessage" -> MessageType.VIDEO;
            case "audiomessage" -> MessageType.AUDIO;
            case "documentmessage" -> MessageType.DOCUMENT;
            case "locationmessage" -> MessageType.LOCATION;
            case "contactmessage" -> MessageType.CONTACT;
            case "stickermessage" -> MessageType.STICKER;
            default -> MessageType.TEXT;
        };
    }
}
