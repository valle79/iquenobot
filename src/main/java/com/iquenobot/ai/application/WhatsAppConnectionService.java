package com.iquenobot.ai.application;

import com.iquenobot.ai.domain.service.IWhatsAppProvider;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.exception.BusinessException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppConnectionService {

    private final SettingRepository settingRepository;
    private final IWhatsAppProvider whatsAppProvider;

    @Value("${app.base-url:http://localhost:8085}")
    private String backendBaseUrl;

    @Value("${EVOLUTION_WEBHOOK_BASE_URL:}")
    private String evolutionWebhookBaseUrl;

    private static final String CATEGORY = "whatsapp";

    @Data
    @Builder
    public static class ConnectionStatusResponse {
        private boolean connected;
        private String instanceId;
        private String provider;
        private String phoneNumber;
        private boolean configured;
        private String error;
    }

    @Data
    @Builder
    public static class TestConnectionResponse {
        private boolean success;
        private boolean connected;
        private String message;
    }

    @Data
    @Builder
    public static class QRCodeResponse {
        private String base64;
        private boolean hasQR;
        private String error;
    }

    @Transactional(readOnly = true)
    public ConnectionStatusResponse getStatus() {
        UUID tenantId = getTenantId();
        List<Setting> settings = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, CATEGORY);

        String instanceId = getSettingValue(settings, "instance_id");
        String provider = getSettingValue(settings, "provider");
        String phoneNumber = getSettingValue(settings, "phone_number");
        String apiKey = getSettingValue(settings, "api_key");

        boolean configured = instanceId != null && !instanceId.isBlank()
                && apiKey != null && !apiKey.isBlank();

        boolean connected = false;
        String error = null;

        if (configured) {
            try {
                connected = whatsAppProvider.isConnected(instanceId);
            } catch (Exception e) {
                log.warn("Error checking WhatsApp connection status: {}", e.getMessage());
                error = "No se pudo verificar el estado de conexión";
            }
        }

        return ConnectionStatusResponse.builder()
                .connected(connected)
                .instanceId(instanceId)
                .provider(provider)
                .phoneNumber(phoneNumber)
                .configured(configured)
                .error(error)
                .build();
    }

    @Transactional
    public TestConnectionResponse testConnection() {
        UUID tenantId = getTenantId();
        List<Setting> settings = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, CATEGORY);

        String instanceId = getSettingValue(settings, "instance_id");
        String apiKey = getSettingValue(settings, "api_key");

        if (instanceId == null || instanceId.isBlank()) {
            return TestConnectionResponse.builder()
                    .success(false)
                    .connected(false)
                    .message("No se ha configurado el ID de instancia de WhatsApp")
                    .build();
        }

        if (apiKey == null || apiKey.isBlank()) {
            return TestConnectionResponse.builder()
                    .success(false)
                    .connected(false)
                    .message("No se ha configurado la API Key de WhatsApp")
                    .build();
        }

        try {
            whatsAppProvider.setWebhook(instanceId, getWebhookUrl(instanceId));

            boolean connected = whatsAppProvider.isConnected(instanceId);
            updateConnectedSetting(tenantId, connected);

            return TestConnectionResponse.builder()
                    .success(true)
                    .connected(connected)
                    .message(connected
                            ? "Conexión exitosa con Evolution API"
                            : "La instancia no está conectada. Verifica que Evolution API esté corriendo y la instancia esté activa.")
                    .build();
        } catch (Exception e) {
            log.error("Error testing WhatsApp connection: {}", e.getMessage(), e);
            updateConnectedSetting(tenantId, false);

            return TestConnectionResponse.builder()
                    .success(false)
                    .connected(false)
                    .message("Error al conectar: " + e.getMessage())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public QRCodeResponse getQRCode() {
        UUID tenantId = getTenantId();
        List<Setting> settings = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, CATEGORY);

        String instanceId = getSettingValue(settings, "instance_id");

        if (instanceId == null || instanceId.isBlank()) {
            return QRCodeResponse.builder()
                    .hasQR(false)
                    .error("No se ha configurado el ID de instancia")
                    .build();
        }

        try {
            whatsAppProvider.setWebhook(instanceId, getWebhookUrl(instanceId));

            String qrBase64 = whatsAppProvider.getQRCode(instanceId);
            if (qrBase64 != null) {
                String fullBase64 = qrBase64.startsWith("data:") ? qrBase64 : "data:image/png;base64," + qrBase64;
                return QRCodeResponse.builder()
                        .base64(fullBase64)
                        .hasQR(true)
                        .build();
            }
            return QRCodeResponse.builder()
                    .hasQR(false)
                    .error("No se pudo generar el código QR")
                    .build();
        } catch (Exception e) {
            log.warn("Error getting QR code: {}", e.getMessage());
            return QRCodeResponse.builder()
                    .hasQR(false)
                    .error("Error al obtener código QR: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public void disconnect() {
        UUID tenantId = getTenantId();
        List<Setting> settings = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, CATEGORY);
        String instanceId = getSettingValue(settings, "instance_id");

        if (instanceId != null && !instanceId.isBlank()) {
            try {
                whatsAppProvider.disconnect(instanceId);
                updateConnectedSetting(tenantId, false);
                log.info("WhatsApp disconnected for tenant: {}", tenantId);
            } catch (Exception e) {
                log.error("Error disconnecting WhatsApp: {}", e.getMessage());
                throw new BusinessException("Error al desconectar WhatsApp: " + e.getMessage());
            }
        }
    }

    @Transactional
    public void updateConnectedSetting(UUID tenantId, boolean connected) {
        String value = String.valueOf(connected);
        var existing = settingRepository.findByTenantIdAndCategoryAndKeyAndDeletedFalse(tenantId, CATEGORY, "connected");
        if (existing.isPresent()) {
            Setting setting = existing.get();
            setting.setValue(value);
            settingRepository.save(setting);
        } else {
            Setting setting = Setting.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .category(CATEGORY)
                    .key("connected")
                    .value(value)
                    .type("boolean")
                    .description("Estado de conexión de WhatsApp")
                    .build();
            settingRepository.save(setting);
        }
    }

    private String getWebhookUrl(String instanceId) {
        String base = evolutionWebhookBaseUrl != null && !evolutionWebhookBaseUrl.isBlank()
                ? evolutionWebhookBaseUrl
                : backendBaseUrl;
        return base + "/api/v1/whatsapp/webhook/" + instanceId;
    }

    private String getSettingValue(List<Setting> settings, String key) {
        return settings.stream()
                .filter(s -> s.getKey().equals(key))
                .map(Setting::getValue)
                .findFirst()
                .orElse(null);
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
