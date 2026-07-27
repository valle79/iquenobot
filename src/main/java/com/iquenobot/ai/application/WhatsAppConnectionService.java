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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppConnectionService {

    private final SettingRepository settingRepository;
    private final IWhatsAppProvider whatsAppProvider;

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
