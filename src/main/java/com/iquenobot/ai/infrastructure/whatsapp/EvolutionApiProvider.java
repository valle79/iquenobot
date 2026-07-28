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

import java.util.HashMap;
import java.util.Map;

/**
 * Evolution API implementation for WhatsApp
 * 
 * Evolution API is an open-source WhatsApp REST API.
 * This implementation can be easily replaced with other providers
 * by implementing the IWhatsAppProvider interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvolutionApiProvider implements IWhatsAppProvider {

    private final RestTemplate restTemplate;

    @Value("${evolution.api.url:http://localhost:8080}")
    private String evolutionApiUrl;

    @Value("${evolution.api.key:}")
    private String apiKey;

    @Value("${app.base-url:http://localhost:8085}")
    private String backendBaseUrl;

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
            log.error("Error sending message via Evolution API: {}", e.getMessage(), e);
            throw new BusinessException("Error al enviar mensaje por WhatsApp: " + e.getMessage());
        }
    }

    @Override
    public String sendMediaMessage(String instanceId, WhatsAppMessageDto message) {
        log.info("Sending media message via Evolution API to: {}", message.getTo());

        try {
            String endpoint = getMediaEndpoint(message.getType());
            String url = String.format("%s/message/%s/%s", evolutionApiUrl, endpoint, instanceId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("number", message.getTo());
            
            if (message.getType() == MessageType.IMAGE || 
                message.getType() == MessageType.VIDEO || 
                message.getType() == MessageType.AUDIO) {
                requestBody.put("media", message.getMediaUrl());
                if (message.getCaption() != null) {
                    requestBody.put("caption", message.getCaption());
                }
            } else if (message.getType() == MessageType.DOCUMENT) {
                requestBody.put("media", message.getMediaUrl());
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
            log.error("Error sending media message via Evolution API: {}", e.getMessage(), e);
            throw new BusinessException("Error al enviar mensaje multimedia: " + e.getMessage());
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
            default -> throw new BusinessException("Unsupported media type: " + type);
        };
    }
}