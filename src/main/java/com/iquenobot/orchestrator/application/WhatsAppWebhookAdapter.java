package com.iquenobot.orchestrator.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZoneOffset;
import java.util.List;
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

        Map<String, Object> data = extractData(payload.getData());

        if (data == null) {
            log.warn("Webhook data is null or unsupported, skipping");
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
    // DATA EXTRACTION (soporta objeto o array)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Object rawData) {
        if (rawData == null) {
            return null;
        }
        if (rawData instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        if (rawData instanceof List<?> list && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        }
        log.warn("Unsupported webhook data type: {}", rawData.getClass());
        return null;
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

    if (data == null) {
        return "Grupo WhatsApp";
    }

    // Evolution puede enviarlo como pushName
    Object pushName = data.get("pushName");
    if (pushName instanceof String p && !p.isBlank()) {
        return p.trim();
    }

    // Fallback
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

    Map<String, Object> data = extractData(payload.getData());

    // -------------------------------------------------------------
    // Identificador de la conversación
    // -------------------------------------------------------------
    String conversationJid = extractConversationJid(data);

    // -------------------------------------------------------------
    // Número real del contacto que participa en la conversación
    // -------------------------------------------------------------
    String senderJid = extractRemoteJid(data);

    if (senderJid == null || senderJid.isBlank()) {
        throw new IllegalArgumentException(
                "Unable to extract WhatsApp source identifier from webhook payload"
        );
    }

    // -------------------------------------------------------------
    // Datos del mensaje
    // -------------------------------------------------------------
    String messageId = extractMessageId(data);
    String pushName = extractPushName(data);
    String messageTypeStr = extractMessageType(data);
    String text = extractText(data);
    Long timestamp = extractTimestamp(data);

    MessageType type = resolveMessageType(messageTypeStr);

    // -------------------------------------------------------------
    // Conversación grupal
    // -------------------------------------------------------------
    boolean isGroup = isGroupConversation(data);
    String groupName = extractGroupName(data);

    // -------------------------------------------------------------
    // Nombre del contacto
    // -------------------------------------------------------------
    // IMPORTANTE:
    // - Mensajes entrantes: usar pushName del remitente.
    // - Mensajes salientes: NO usar pushName porque será tu propio nombre
    //   (LuisDev). En ese caso usamos el número del contacto y luego el
    //   ContactResolutionStep obtendrá el nombre real desde la base de datos.
    // -------------------------------------------------------------
    String sourceName;

    if (outbound) {
        sourceName = senderJid;
    } else {
        sourceName = (pushName != null && !pushName.isBlank())
                ? pushName.trim()
                : senderJid;
    }

    // -------------------------------------------------------------
    // Construcción del mensaje
    // -------------------------------------------------------------
    return IncomingMessage.builder()
            .channelMessageId(messageId)
            .channel(ChannelType.WHATSAPP)

            // Conversación (grupo o chat individual)
            .channelConversationId(
                    conversationJid != null ? cleanJid(conversationJid) : null
            )

            // Contacto real
            .sourceIdentifier(senderJid)
            .sourceName(sourceName)

            .type(type)
            .content(text != null ? text : "")
            .instanceId(instanceId)

            // Nombre del grupo si aplica
            .conversationName(isGroup ? groupName : null)

            .metadata(data)
            .outbound(outbound)

            .timestamp(timestamp != null
                    ? LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
                    : LocalDateTime.now(ZoneOffset.UTC))

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
        // 🔥 Detectar si es grupo
        // -------------------------------------------------------------
        Object remoteJidObj = key.get("remoteJid");
        String remoteJid = remoteJidObj instanceof String ? (String) remoteJidObj : null;

        boolean group = remoteJid != null && remoteJid.contains("@g.us");

        // -------------------------------------------------------------
        // 📱 CHAT INDIVIDUAL
        // -------------------------------------------------------------
        if (!group) {

            // Usar siempre remoteJidAlt si existe
            Object remoteJidAlt = key.get("remoteJidAlt");
            if (remoteJidAlt instanceof String alt && !alt.isBlank()) {
                return normalizePhone(cleanJid(alt));
            }

            // Fallback: remoteJid
            if (remoteJid != null && !remoteJid.isBlank()) {
                return normalizePhone(cleanJid(remoteJid));
            }
        }

        // -------------------------------------------------------------
        // 👥 GRUPOS
        // -------------------------------------------------------------
        else {

            // Para grupos, el contacto es quien participa
            Object participant = key.get("participant");
            if (participant instanceof String p && !p.isBlank()) {
                return normalizePhone(cleanJid(p));
            }

            // Fallback raro
            if (remoteJid != null && !remoteJid.isBlank()) {
                return normalizePhone(cleanJid(remoteJid));
            }
        }
    }

    // -------------------------------------------------------------
    // Fallback Evolution API
    // -------------------------------------------------------------
    Object source = data.get("source");
    if (source instanceof String s && !s.isBlank()) {
        return normalizePhone(cleanJid(s));
    }

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
    return PhoneNormalizer.normalize(value);
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
