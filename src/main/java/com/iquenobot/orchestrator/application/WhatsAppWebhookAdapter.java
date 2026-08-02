package com.iquenobot.orchestrator.application;

import com.iquenobot.ai.domain.dto.WhatsAppWebhookDto;
import com.iquenobot.ai.infrastructure.whatsapp.EvolutionApiProvider;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingResult;
import com.iquenobot.shared.application.FileUploadService;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookAdapter {

    /**
     * Referencia global de arranque: cualquier webhook (redelivery de Evolution)
     * con messageTimestamp anterior a este instante corresponde a mensajes
     * históricos y se descarta para evitar reprocesar y responder mensajes viejos.
     */
    private final Instant applicationStartedAt = Instant.now();

    private final ConversationOrchestrator orchestrator;
    private final ConversationMessageRepository messageRepository;
    private final EvolutionApiProvider evolutionApiProvider;
    private final FileUploadService fileUploadService;

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
        // MENSAJES ANTERIORES AL ARRANQUE: descartar redeliveries históricas
        // ---------------------------------------------------------------------
        Long messageTimestamp = extractTimestamp(data);
        if (messageTimestamp != null
                && Instant.ofEpochSecond(messageTimestamp).isBefore(applicationStartedAt)) {
            log.info("Skipping webhook with old messageTimestamp={} (before application startup)",
                    messageTimestamp);
            return ProcessingResult.empty();
        }

        // ---------------------------------------------------------------------
        // DEDUP: evitar procesar mensajes ya recibidos
        // ---------------------------------------------------------------------
        String messageId = extractMessageId(data);

        // Algunos webhooks llegan sin messageId (redelivery de Evolution).
        // Se genera un ID determinístico a partir del contenido + timestamp
        // para que las redelivery del mismo evento se descarten.
        if (messageId == null || messageId.isBlank()) {
            messageId = generateSyntheticMessageId(data);
        }

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

    if (data == null) {
        return false;
    }

    if (data.get("key") instanceof Map<?, ?> key) {
        Object remoteJid = key.get("remoteJid");
        return remoteJid instanceof String r && r.endsWith("@g.us");
    }

    return false;
}

@SuppressWarnings("unchecked")
private String extractGroupName(String instanceId, Map<String, Object> data) {

    if (data == null || !isGroupConversation(data)) {
        return null;
    }

    // 1. Algunos eventos sí traen subject
    Object subject = data.get("subject");
    if (subject instanceof String s && !s.isBlank()) {
        return s.trim();
    }

    // 2. Intentar obtenerlo desde message.groupName
    if (data.get("message") instanceof Map<?, ?> message) {
        Object groupName = message.get("groupName");
        if (groupName instanceof String g && !g.isBlank()) {
            return g.trim();
        }
    }

    // 3. Resolver desde Evolution API
    try {
        String groupJid = extractConversationJid(data);

        if (groupJid != null) {
            String resolvedName = evolutionApiProvider.getGroupName(instanceId, groupJid);

            if (resolvedName != null && !resolvedName.isBlank()) {
                log.info("Nombre del grupo resuelto desde Evolution: {} -> {}",
                        groupJid, resolvedName);
                return resolvedName.trim();
            }
        }

    } catch (Exception e) {
        log.warn("No se pudo resolver el nombre del grupo: {}", e.getMessage());
    }

    // 4. Fallback legible
    String jid = extractConversationJid(data);
    if (jid != null && jid.contains("@g.us")) {
        return "Grupo " + jid.replace("@g.us", "");
    }

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
    String conversationJid = extractConversationId(data);

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
    // Datos multimedia (imagen, video, audio, documento, sticker)
    // -------------------------------------------------------------
    String mediaUrl = extractMediaUrl(data);
    String caption = extractCaption(data);
    String filename = extractFileName(data);
    String mimeType = extractMimeType(data);
    String mediaKey = extractMediaKey(data);
    Integer durationSeconds = extractDurationSeconds(data);

    // -------------------------------------------------------------
    // Media (entrante o saliente desde el celular): descargar el
    // contenido real de WhatsApp (las URLs .enc expiran y están
    // encriptadas). Evolution lo descarga y desencripta usando la
    // sesión, y lo guardamos en el storage local (FileUploadService).
    // -------------------------------------------------------------
    if (mediaUrl != null && !mediaUrl.isBlank() && type != MessageType.TEXT) {
        String localMediaUrl = downloadInboundMedia(instanceId, data, filename, mimeType);
        if (localMediaUrl != null) {
            mediaUrl = localMediaUrl;
        }
    }

    // -------------------------------------------------------------
    // Conversación grupal
    // -------------------------------------------------------------
boolean isGroup = isGroupConversation(data);
String groupName = extractGroupName(instanceId, data);

    // -------------------------------------------------------------
    // Nombre del contacto
    // -------------------------------------------------------------
    // IMPORTANTE:
    // - Mensajes entrantes: usar pushName del remitente.
    // - Mensajes salientes en grupos: usar el pushName del remitente
    //   (quien escribe desde el celular, p.ej. "LuisDev").
    // - Mensajes salientes en chats individuales: NO usar pushName
    //   (sería tu propio nombre); se usa el número del contacto y luego
    //   el ContactResolutionStep obtendrá el nombre real desde la base.
    // -------------------------------------------------------------
    String sourceName;

    if (outbound) {
        sourceName = (isGroup && pushName != null && !pushName.isBlank())
                ? pushName.trim()
                : senderJid;
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

        // Mantener el JID completo del grupo
        .channelConversationId(conversationJid)

        // Contacto real que escribió
        .sourceIdentifier(senderJid)
        .sourceName(sourceName)

        .type(type)
        .content(text != null ? text : "")

        .mediaUrl(mediaUrl)
        .caption(caption)
        .filename(filename)
        .mimeType(mimeType)
        .channelMediaId(mediaKey)
        .durationSeconds(durationSeconds)

        .instanceId(instanceId)

        // Nombre del grupo
        .conversationName(groupName)

        .metadata(data)
        .outbound(outbound)
        .group(isGroup)

        .timestamp(timestamp != null
                ? LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC)
                : LocalDateTime.now(ZoneOffset.UTC))

        .build();
}

    /**
     * Descarga y persiste el media de un mensaje entrante.
     * Devuelve la URL local del archivo, o null si no se pudo descargar
     * (en ese caso se conserva la URL remota .enc del webhook).
     */
    private String downloadInboundMedia(String instanceId, Map<String, Object> data,
                                        String filename, String mimeType) {
        try {
            Map<String, Object> messageInfo = new HashMap<>();
            if (data.get("key") != null) {
                messageInfo.put("key", data.get("key"));
            }
            if (data.get("message") != null) {
                messageInfo.put("message", data.get("message"));
            }

            Map<String, Object> media = evolutionApiProvider.downloadMedia(instanceId, messageInfo);
            if (media == null) {
                log.warn("No se pudo descargar el media entrante, se conserva la URL remota");
                return null;
            }

            Object base64Obj = media.get("base64");
            if (!(base64Obj instanceof String base64) || base64.isBlank()) {
                log.warn("Media entrante sin contenido base64, se conserva la URL remota");
                return null;
            }

            byte[] bytes = Base64.getDecoder().decode(base64);

            String mediaFileName = media.get("fileName") instanceof String f && !f.isBlank()
                    ? f : filename;
            String mediaMimeType = media.get("mimetype") instanceof String m && !m.isBlank()
                    ? m : mimeType;

            String localUrl = fileUploadService.uploadBytes(bytes, mediaFileName, mediaMimeType);
            log.info("Media entrante descargado y almacenado localmente: {}", localUrl);
            return localUrl;
        } catch (Exception e) {
            log.warn("Error descargando media entrante, se conserva la URL remota: {}", e.getMessage());
            return null;
        }
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

        Object remoteJidObj = key.get("remoteJid");
        String remoteJid = remoteJidObj instanceof String ? (String) remoteJidObj : null;

        // =============================================================
        // CHAT INDIVIDUAL
        // =============================================================
        if (remoteJid != null && remoteJid.endsWith("@s.whatsapp.net")) {
            return normalizePhone(cleanJid(remoteJid));
        }

        // =============================================================
        // GRUPO
        // =============================================================
        if (remoteJid != null && remoteJid.endsWith("@g.us")) {

            // usar participant SOLO como contacto que envió el mensaje
            Object participant = key.get("participant");

            if (participant instanceof String p && !p.isBlank()) {
                return normalizePhone(cleanJid(p));
            }

            // fallback extremo
            return remoteJid;
        }
    }

    return null;
}

@SuppressWarnings("unchecked")
private String extractConversationId(Map<String, Object> data) {

    if (data == null) {
        return null;
    }

    if (data.get("key") instanceof Map<?, ?> key) {

        Object remoteJid = key.get("remoteJid");

        if (remoteJid instanceof String r && !r.isBlank()) {
            return r.trim(); // NO limpiar @g.us
        }
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
    // SYNTHETIC MESSAGE ID (para webhooks sin key.id)
    // -------------------------------------------------------------------------

    private String generateSyntheticMessageId(Map<String, Object> data) {
        String remoteJid = extractRemoteJid(data);
        String content = extractText(data);
        Long timestamp = extractTimestamp(data);

        String raw = (remoteJid != null ? remoteJid : "")
                + "|" + (content != null ? content : "")
                + "|" + (timestamp != null ? timestamp : "");

        if (raw.isBlank() || raw.equals("||")) {
            return null;
        }

        return "SYN-" + Integer.toHexString(raw.hashCode());
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

            // Audio, sticker y otros media: sin texto propio
            if (message.get("audioMessage") instanceof Map<?, ?>
                    || message.get("stickerMessage") instanceof Map<?, ?>
                    || message.get("locationMessage") instanceof Map<?, ?>
                    || message.get("ptvMessage") instanceof Map<?, ?>) {
                return null;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // MEDIA EXTRACTION
    // -------------------------------------------------------------------------

    private Map<?, ?> extractMediaMessage(Map<String, Object> data) {
        if (data == null || !(data.get("message") instanceof Map<?, ?> message)) {
            return null;
        }

        for (String key : new String[]{
                "imageMessage", "videoMessage", "audioMessage",
                "documentMessage", "stickerMessage", "ptvMessage"}) {
            if (message.get(key) instanceof Map<?, ?> media) {
                return media;
            }
        }

        return null;
    }

    private String extractMediaUrl(Map<String, Object> data) {
        Map<?, ?> media = extractMediaMessage(data);
        return media != null ? asString(media.get("url")) : null;
    }

    private String extractCaption(Map<String, Object> data) {
        Map<?, ?> media = extractMediaMessage(data);
        return media != null ? asString(media.get("caption")) : null;
    }

    private String extractFileName(Map<String, Object> data) {
        Map<?, ?> media = extractMediaMessage(data);
        return media != null ? asString(media.get("fileName")) : null;
    }

    private String extractMimeType(Map<String, Object> data) {
        Map<?, ?> media = extractMediaMessage(data);
        if (media == null) {
            return null;
        }
        String mime = asString(media.get("mimetype"));
        if (mime == null) {
            mime = asString(media.get("mimeType"));
        }
        return mime;
    }

    private String extractMediaKey(Map<String, Object> data) {
        Map<?, ?> media = extractMediaMessage(data);
        return media != null ? asString(media.get("mediaKey")) : null;
    }

    private Integer extractDurationSeconds(Map<String, Object> data) {
        Map<?, ?> media = extractMediaMessage(data);
        if (media == null) {
            return null;
        }
        Object seconds = media.get("seconds");
        if (seconds instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private String asString(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            return s.trim();
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
