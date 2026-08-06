package com.iquenobot.ai.infrastructure.whatsapp;

import com.iquenobot.ai.domain.dto.WhatsAppMessageDto;
import com.iquenobot.ai.domain.service.IWhatsAppProvider;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.WhatsAppProvider;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evolution API implementation for WhatsApp
 * 
 * Evolution API is an open-source WhatsApp REST API.
 * This implementation can be easily replaced with other providers
 * by implementing the IWhatsAppProvider interface.
 */@Service
@RequiredArgsConstructor
@Slf4j
public class EvolutionApiProvider implements IWhatsAppProvider {

    private final RestTemplate restTemplate;

    @Value("${evolution.api.url:http://localhost:8081}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key:}")
    private String apiKey;

    @Value("${app.base-url:http://localhost:8085}")
    private String backendBaseUrl;

    @Override
    public String createInstance(String instanceName) {
        log.info("Creating WhatsApp instance via Evolution API: {}", instanceName);

        try {
            String url = evolutionApiUrl + "/instance/create";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("instanceName", instanceName);
            requestBody.put("integration", "WHATSAPP-BAILEYS");
            requestBody.put("qrcode", true);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object rawInstance = response.getBody().get("instance");
                if (rawInstance instanceof Map<?, ?> instance) {
                    Object name = instance.get("instanceName");
                    if (name instanceof String s && !s.isBlank()) {
                        log.info("WhatsApp instance created successfully: {}", s);
                        return s;
                    }
                }
            }

            throw new BusinessException("No se pudo crear la instancia de WhatsApp");

        } catch (Exception e) {
            String detail = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (detail.contains("exists") || detail.contains("already") || detail.contains("ya existe")) {
                log.warn("WhatsApp instance already exists, reusing: {}", instanceName);
                return instanceName;
            }
            log.error("Error creating WhatsApp instance {}: {}", instanceName, e.getMessage());
            throw new BusinessException("Error al crear la instancia de WhatsApp: " + e.getMessage());
        }
    }

    @Override
    public String sendMessage(String instanceId, WhatsAppMessageDto message) {
        log.info("Sending text message via Evolution API to: {}", message.getTo());

        try {
            String url = String.format("%s/message/sendText/%s", evolutionApiUrl, instanceId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("number", message.getTo());
            requestBody.put("text", message.getText());
            
            if (message.getReplyToMessageId() != null) {
                Map<String, String> options = new HashMap<>();
                options.put("quoted", message.getReplyToMessageId());
                requestBody.put("options", options);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = responseBody.get("key") != null ? 
                        ((Map<String, String>) responseBody.get("key")).get("id") : null;
                
                log.info("Message sent successfully. ID: {}", messageId);
                return messageId;
            }

            throw new BusinessException("Failed to send message via Evolution API");

        } catch (Exception e) {
            String detail = e.getMessage();
            log.error("Error sending message via Evolution API: {}", detail, e);
            throw new BusinessException(buildSendErrorMessage(detail, message.getTo()));
        }
    }

    /**
     * Traduce un error de Evolution API a un mensaje claro y accionable.
     * Caso común: el destinatario no existe o no tiene WhatsApp activo
     * (Evolution responde "exists": false).
     */
    private String buildSendErrorMessage(String detail, String recipient) {
        if (detail != null && detail.contains("exists")) {
            return "El número " + recipient + " no existe o no tiene WhatsApp activo. Verifica el número del contacto.";
        }
        if (detail != null && detail.contains("400")) {
            return "No se pudo enviar el mensaje por WhatsApp (400). Verifica que el número sea válido.";
        }
        return "Error al enviar mensaje por WhatsApp: " + detail;
    }



public String getGroupName(String instanceId, String groupJid) {

    if (instanceId == null || instanceId.isBlank() || groupJid == null || groupJid.isBlank()) {
        return null;
    }

    // Evolution API v2.3.x: GET /group/findGroupInfos/{instance}?groupJid=...
    // Respuesta: objeto del grupo { id, subject, ... }
    try {
        java.net.URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(evolutionApiUrl)
                .path("/group/findGroupInfos/{instance}")
                .queryParam("groupJid", groupJid)
                .buildAndExpand(instanceId)
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                Map.class
        );

        String subject = extractSubject(response.getBody());

        if (subject != null) {
            return subject;
        }

    } catch (Exception e) {
        log.warn("Error obteniendo nombre del grupo {} (GET): {}", groupJid, e.getMessage());
    }

    // Fallback Evolution API v2 (otras versiones): POST /group/findGroupInfos/{instance} con "groupJids" (array)
    try {
        String url = evolutionApiUrl + "/group/findGroupInfos/" + instanceId;

        Map<String, Object> payload = Map.of(
                "groupJids", java.util.List.of(groupJid)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );

        String subject = extractSubject(response.getBody());

        if (subject != null) {
            return subject;
        }

    } catch (Exception e) {
        log.warn("Error obteniendo nombre del grupo {} (POST): {}", groupJid, e.getMessage());
    }

    return null;
}

    /**
     * Extrae el subject de la respuesta de findGroupInfos (v1 y v2).
     * v2 responde un array de grupos; v1 responde el array o un objeto.
     */
    @SuppressWarnings("unchecked")
    private String extractSubject(Map body) {
        if (body == null) {
            return null;
        }

        Object candidate = body;

        if (body.get("groups") instanceof java.util.List<?> groups && !groups.isEmpty()) {
            candidate = groups.get(0);
        }

        if (candidate instanceof java.util.List<?> list && !list.isEmpty()) {
            candidate = list.get(0);
        }

        if (candidate instanceof Map<?, ?> group) {
            Object subject = group.get("subject");
            if (subject instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }

        return null;
    }

    @Override
    public String sendMediaMessage(String instanceId, WhatsAppMessageDto message) {
        log.info("Sending media message via Evolution API to: {}", message.getTo());

        try {
            String endpoint = getMediaEndpoint(message.getType());
            String url = String.format("%s/message/%s/%s", evolutionApiUrl, endpoint, instanceId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("number", message.getTo());

            // Evolution valida "media" como URL o base64 (class-validator isURL
            // con require_tld=true rechaza localhost). Las URLs locales se
            // envían como base64 para evitar el 400 "Owned media must be a url or base64".
            if (message.getType() == MessageType.STICKER) {
                requestBody.put("sticker", resolveMediaPayload(message.getMediaUrl()));
            } else if (message.getType() == MessageType.AUDIO) {
                // Evolution v2.x: /message/sendWhatsAppAudio espera el campo "audio"
                requestBody.put("audio", resolveMediaPayload(message.getMediaUrl()));
            } else if (message.getType() == MessageType.IMAGE) {
                requestBody.put("mediatype", "image");
                requestBody.put("media", resolveMediaPayload(message.getMediaUrl()));
                if (message.getCaption() != null) {
                    requestBody.put("caption", message.getCaption());
                }
            } else if (message.getType() == MessageType.VIDEO) {
                requestBody.put("mediatype", "video");
                requestBody.put("media", resolveMediaPayload(message.getMediaUrl()));
                if (message.getCaption() != null) {
                    requestBody.put("caption", message.getCaption());
                }
            } else if (message.getType() == MessageType.DOCUMENT) {
                requestBody.put("mediatype", "document");
                requestBody.put("media", resolveMediaPayload(message.getMediaUrl()));
                requestBody.put("fileName", message.getFilename());
                if (message.getCaption() != null) {
                    requestBody.put("caption", message.getCaption());
                }
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = responseBody.get("key") != null ? 
                        ((Map<String, String>) responseBody.get("key")).get("id") : null;
                
                log.info("Media message sent successfully. ID: {}", messageId);
                return messageId;
            }

            throw new BusinessException("Failed to send media message via Evolution API");

        } catch (Exception e) {
            String detail = e.getMessage();
            log.error("Error sending media message via Evolution API: {}", detail, e);
            throw new BusinessException(buildSendErrorMessage(detail, message.getTo()));
        }
    }

    /**
     * Descarga el contenido multimedia de un mensaje entrante usando
     * Evolution API v2.x: POST /chat/getBase64FromMediaMessage/{instance}
     *
     * El body debe contener el WebMessageInfo tal como llega en el webhook
     * ({key, message}). Evolution usa sus credenciales de sesión para
     * desencriptar la URL .enc de WhatsApp.
     *
     * @return mapa con mediaType, fileName, caption, mimetype y base64,
     *         o null si no hay media válida.
     */
    public Map<String, Object> downloadMedia(String instanceId, Map<String, Object> messageInfo) {
        try {
            String url = String.format("%s/chat/getBase64FromMediaMessage/%s", evolutionApiUrl, instanceId);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", messageInfo);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Object base64 = body.get("base64");
                if (base64 instanceof String s && !s.isBlank()) {
                    log.info("Media downloaded from Evolution API. type={} filename={}",
                            body.get("mediaType"), body.get("fileName"));
                    return body;
                }
                log.warn("Evolution returned no base64 media content");
                return null;
            }

            log.warn("Failed to download media via Evolution API. Status: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Error downloading media via Evolution API: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void markAsRead(String instanceId, String messageId) {
        log.info("Marking message as read via Evolution API: {}", messageId);

        try {
            String url = String.format("%s/chat/markMessageAsRead/%s", evolutionApiUrl, instanceId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("readMessages", new String[]{messageId});

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, request, Void.class);
            
            log.info("Message marked as read successfully");

        } catch (Exception e) {
            log.warn("Error marking message as read via Evolution API: {}", e.getMessage());
            // Non-critical error, don't throw exception
        }
    }

    @Override
    public boolean isConnected(String instanceId) {
        try {
            String url = String.format("%s/instance/connectionState/%s", evolutionApiUrl, instanceId);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> instance = (Map<String, Object>) response.getBody().get("instance");
                if (instance != null) {
                    String state = (String) instance.get("state");
                    return "open".equalsIgnoreCase(state);
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Error checking connection status: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getQRCode(String instanceId) {
        try {
            String url = String.format("%s/instance/connect/%s", evolutionApiUrl, instanceId);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("base64");
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting QR code: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void disconnect(String instanceId) {
        try {
            String url = String.format("%s/instance/logout/%s", evolutionApiUrl, instanceId);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            
            log.info("Instance disconnected successfully: {}", instanceId);

        } catch (Exception e) {
            log.error("Error disconnecting instance: {}", e.getMessage());
            throw new BusinessException("Error al desconectar instancia de WhatsApp");
        }
    }

    @Override
    public WhatsAppProvider getProviderType() {
        return WhatsAppProvider.EVOLUTION_API;
    }

    @Override
    public boolean validateWebhookSignature(String payload, String signature) {
        return true;
    }

    @Override
    public void setWebhook(String instanceId, String webhookUrl) {
        log.info("=== CONFIGURANDO WEBHOOK Evolution API ===");
        log.info("Instance: {} -> URL: {}", instanceId, webhookUrl);

        try {
            String url = String.format("%s/webhook/set/%s", evolutionApiUrl, instanceId);

            Map<String, Object> webhookConfig = new HashMap<>();
            webhookConfig.put("enabled", true);
            webhookConfig.put("url", webhookUrl);
            webhookConfig.put("webhookByEvents", false);
            webhookConfig.put("webhookBase64", false);
            webhookConfig.put("base64", false);
            webhookConfig.put("events", java.util.List.of("MESSAGES_UPSERT", "MESSAGES_SET", "MESSAGES_DELETE"));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("webhook", webhookConfig);

            log.debug("Webhook request payload: {}", requestBody);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("=== WEBHOOK CONFIGURADO EXITOSAMENTE para instancia: {} ===", instanceId);
                log.debug("Evolution API response: {}", response.getBody());
            } else {
                log.warn("Error configurando webhook. Status: {} Response: {}", response.getStatusCode(), response.getBody());
            }

        } catch (Exception e) {
            log.error("Error configurando webhook para instancia {}: {}", instanceId, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // RESOLUCIÓN DE LID (privacidad de número de WhatsApp)
    // -------------------------------------------------------------------------

    /**
     * TTL de la caché de contactos: WhatsApp puede tirar muchos mensajes
     * seguidos y no conviene llamar a Evolution por cada uno. La resolución
     * es una optimización de pocos segundos, no requiere frescura total.
     */
    private static final Duration CONTACTS_CACHE_TTL = Duration.ofSeconds(30);
    private final Map<String, CachedContacts> contactsCache = new ConcurrentHashMap<>();

    /**
     * Resuelve el número real de un JID "user@lid" (privacidad de WhatsApp).
     *
     * Evolution conserva en su contact store las dos identidades del mismo
     * contacto: el JID "@lid" y el JID real "@s.whatsapp.net". Al consultarlo
     * se puede vincular el mensaje privado al contacto existente (mismo
     * número, mismo historial) en lugar de crear un duplicado.
     *
     * @return JID real "55...@s.whatsapp.net" o null si no hay coincidencia.
     */
    public String resolveLidToPhoneNumber(String instanceId, String lidJid, String pushName) {
        if (instanceId == null || instanceId.isBlank()
                || lidJid == null || !lidJid.endsWith("@lid")) {
            return null;
        }

        try {
            List<Map<String, Object>> contacts = fetchContacts(instanceId);
            if (contacts == null || contacts.isEmpty()) {
                return null;
            }

            String nameToMatch = findContactName(contacts, lidJid);
            if (nameToMatch == null) {
                nameToMatch = pushName;
            }
            if (nameToMatch == null || nameToMatch.isBlank()) {
                return null;
            }

            for (Map<String, Object> contact : contacts) {
                String jid = contactJid(contact);
                if (jid != null && jid.endsWith("@s.whatsapp.net")
                        && nameToMatch.equalsIgnoreCase(contactName(contact))) {
                    return jid;
                }
            }
        } catch (Exception e) {
            log.warn("Error resolviendo LID {} a número real: {}", lidJid, e.getMessage());
        }

        return null;
    }

    /**
     * Obtiene la lista de contactos conocidos por la instancia con un caché
     * corto (TTL 30s). Si la llamada falla, se reutiliza lo que haya en caché.
     */
    private List<Map<String, Object>> fetchContacts(String instanceId) {
        CachedContacts cached = contactsCache.get(instanceId);
        if (cached != null && !cached.fetchedAt().plus(CONTACTS_CACHE_TTL).isBefore(Instant.now())) {
            return cached.contacts();
        }

        try {
            String url = evolutionApiUrl + "/chat/findContacts/" + instanceId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);
            HttpEntity<String> request = new HttpEntity<>("{}", headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Object.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> contacts = parseContactList(response.getBody());
                if (contacts != null) {
                    contactsCache.put(instanceId, new CachedContacts(Instant.now(), contacts));
                    return contacts;
                }
            }
        } catch (Exception e) {
            log.warn("Error obteniendo contactos de Evolution: {}", e.getMessage());
        }

        // fallback suave: se conserva lo que haya en caché aunque esté expirado
        return cached != null ? cached.contacts() : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseContactList(Object body) {
        if (body instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result.isEmpty() ? null : result;
        }

        if (body instanceof Map<?, ?> map) {
            for (String key : new String[]{"contacts", "jids"}) {
                if (map.get(key) instanceof List<?> list) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> entry) {
                            result.add((Map<String, Object>) entry);
                        }
                    }
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    private String findContactName(List<Map<String, Object>> contacts, String lidJid) {
        for (Map<String, Object> contact : contacts) {
            if (lidJid.equalsIgnoreCase(contactJid(contact))) {
                return contactName(contact);
            }
        }
        return null;
    }

    private String contactJid(Map<String, Object> contact) {
        for (String key : new String[]{"remoteJid", "jid"}) {
            if (contact.get(key) instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }

    private String contactName(Map<String, Object> contact) {
        for (String key : new String[]{"pushName", "name"}) {
            if (contact.get(key) instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }

    private static final class CachedContacts {
        private final Instant fetchedAt;
        private final List<Map<String, Object>> contacts;

        private CachedContacts(Instant fetchedAt, List<Map<String, Object>> contacts) {
            this.fetchedAt = fetchedAt;
            this.contacts = contacts;
        }

        public Instant fetchedAt() {
            return fetchedAt;
        }

        public List<Map<String, Object>> contacts() {
            return contacts;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("apikey", apiKey);
        }
        return headers;
    }

    private String getMediaEndpoint(MessageType type) {
        return switch (type) {
            case IMAGE -> "sendMedia";
            case VIDEO -> "sendMedia";
            case AUDIO -> "sendWhatsAppAudio";
            case DOCUMENT -> "sendMedia";
            case STICKER -> "sendSticker";
            default -> throw new BusinessException("Unsupported media type: " + type);
        };
    }

    /**
     * Evolution API valida "media"/"audio"/"sticker" como URL o base64
     * (class-validator isURL con require_tld=true: rechaza "localhost").
     * Para URLs locales/privadas descargamos el archivo y lo enviamos
     * como base64. Las URLs públicas se envían tal cual.
     */
    private String resolveMediaPayload(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            throw new BusinessException("La URL del media es requerida");
        }

        String host = null;
        try {
            var uri = java.net.URI.create(mediaUrl);
            host = uri.getHost();
        } catch (Exception ignored) {
            // Si no es una URL válida, se asume base64
        }

        boolean isLocal = host == null
                || host.equalsIgnoreCase("localhost")
                || host.equals("127.0.0.1")
                || host.equals("::1")
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.startsWith("172.")
                || host.equals("[::1]");

        if (!isLocal) {
            return mediaUrl;
        }

        try {
            byte[] bytes = restTemplate.getForObject(mediaUrl, byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new BusinessException("No se pudo descargar el media local: " + mediaUrl);
            }
            log.info("Media local convertido a base64 para Evolution ({} bytes): {}", bytes.length, mediaUrl);
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warn("Error descargando media local {}: {}", mediaUrl, e.getMessage());
            // Si no se puede descargar, se envía la URL tal cual
            return mediaUrl;
        }
    }
}