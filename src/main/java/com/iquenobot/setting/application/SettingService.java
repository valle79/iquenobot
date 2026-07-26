package com.iquenobot.setting.application;

import com.iquenobot.setting.domain.dto.SettingDto;
import com.iquenobot.setting.domain.dto.UpdateSettingsRequestDto;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingService {

    private final SettingRepository settingRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    @Transactional
    public void seedExistingTenants() {
        List<UUID> tenantIds = jdbcTemplate.queryForList(
                "SELECT id FROM tenants WHERE is_deleted = false", UUID.class);
        for (UUID tenantId : tenantIds) {
            boolean hasSettings = settingRepository.findByTenantIdAndDeletedFalse(tenantId).isEmpty() == false;
            if (!hasSettings) {
                log.info("Seeding default settings for existing tenant: {}", tenantId);
                seedDefaults(tenantId);
            }
        }
    }

    private void seedDefaults(UUID tenantId) {
        var defaults = List.of(
                Map.<String, String>of("category", "ai", "key", "provider", "value", "openai", "type", "select", "desc", "Proveedor de IA"),
                Map.<String, String>of("category", "ai", "key", "model", "value", "gpt-4", "type", "string", "desc", "Modelo de IA"),
                Map.<String, String>of("category", "ai", "key", "temperature", "value", "0.7", "type", "string", "desc", "Temperatura del modelo"),
                Map.<String, String>of("category", "ai", "key", "max_tokens", "value", "2048", "type", "string", "desc", "Máximo de tokens"),
                Map.<String, String>of("category", "ai", "key", "auto_reply", "value", "false", "type", "boolean", "desc", "Respuesta automática"),
                Map.<String, String>of("category", "notifications", "key", "email_notifications", "value", "true", "type", "boolean", "desc", "Notificaciones por email"),
                Map.<String, String>of("category", "notifications", "key", "push_notifications", "value", "true", "type", "boolean", "desc", "Notificaciones push"),
                Map.<String, String>of("category", "notifications", "key", "sms_notifications", "value", "false", "type", "boolean", "desc", "Notificaciones SMS"),
                Map.<String, String>of("category", "notifications", "key", "new_message_alert", "value", "true", "type", "boolean", "desc", "Alertas de nuevos mensajes"),
                Map.<String, String>of("category", "notifications", "key", "conversation_assigned_alert", "value", "true", "type", "boolean", "desc", "Alerta al ser asignado a conversación"),
                Map.<String, String>of("category", "general", "key", "language", "value", "es", "type", "select", "desc", "Idioma"),
                Map.<String, String>of("category", "general", "key", "timezone", "value", "America/Mexico_City", "type", "select", "desc", "Zona horaria"),
                Map.<String, String>of("category", "general", "key", "currency", "value", "MXN", "type", "select", "desc", "Moneda"),
                Map.<String, String>of("category", "general", "key", "business_hours_start", "value", "09:00", "type", "string", "desc", "Horario laboral inicio"),
                Map.<String, String>of("category", "general", "key", "business_hours_end", "value", "18:00", "type", "string", "desc", "Horario laboral fin")
        );
        for (var entry : defaults) {
            Setting setting = Setting.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .category(entry.get("category"))
                    .key(entry.get("key"))
                    .value(entry.get("value"))
                    .type(entry.get("type"))
                    .description(entry.get("desc"))
                    .build();
            settingRepository.save(setting);
        }
    }

    @Transactional(readOnly = true)
    public List<SettingDto> getAll() {
        UUID tenantId = getTenantId();
        return settingRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public List<SettingDto> getByCategory(String category) {
        UUID tenantId = getTenantId();
        var results = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, category);
        if (results.isEmpty()) {
            boolean anySettings = settingRepository.findByTenantIdAndDeletedFalse(tenantId).isEmpty() == false;
            if (!anySettings) {
                seedDefaults(tenantId);
                results = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, category);
            }
        }
        return results.stream().map(this::toDto).toList();
    }

    @Transactional
    public List<SettingDto> updateSettings(UpdateSettingsRequestDto request) {
        UUID tenantId = getTenantId();
        String userIdStr = TenantContext.getUserId();
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

        if (request.getSettings() == null || request.getSettings().isEmpty()) {
            return getAll();
        }

        for (UpdateSettingsRequestDto.SettingEntry entry : request.getSettings()) {
            String category = request.getCategory();
            String key = entry.getKey();
            String value = entry.getValue();
            String type = entry.getType() != null ? entry.getType() : "string";

            var existing = settingRepository.findByTenantIdAndCategoryAndKeyAndDeletedFalse(tenantId, category, key);

            if (existing.isPresent()) {
                Setting setting = existing.get();
                setting.setValue(value);
                setting.setType(type);
                settingRepository.save(setting);
            } else {
                Setting setting = Setting.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .category(category)
                        .key(key)
                        .value(value)
                        .type(type)
                        .build();
                settingRepository.save(setting);
            }
        }

        log.info("Settings updated for category: {} in tenant: {}", request.getCategory(), tenantId);
        return getByCategory(request.getCategory());
    }

    private SettingDto toDto(Setting setting) {
        return SettingDto.builder()
                .id(setting.getId())
                .category(setting.getCategory())
                .key(setting.getKey())
                .value(setting.getValue())
                .type(setting.getType())
                .description(setting.getDescription())
                .build();
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
