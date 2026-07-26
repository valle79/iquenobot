package com.iquenobot.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.role.domain.entity.Role;
import com.iquenobot.role.domain.repository.RoleRepository;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDataSeeder {

    private final RoleRepository roleRepository;
    private final SettingRepository settingRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultRoles(UUID tenantId) {
        List<Role> defaultRoles = List.of(
                createRole(tenantId, "TENANT_ADMIN", "Administrador", "Acceso completo a la configuración de la empresa", true),
                createRole(tenantId, "SUPERVISOR", "Supervisor", "Puede gestionar usuarios, conversaciones y reportes", true),
                createRole(tenantId, "AGENT", "Agente", "Puede atender conversaciones y gestionar contactos", true),
                createRole(tenantId, "BOT", "Bot", "Rol automático para el chatbot de IA", true)
        );
        roleRepository.saveAll(defaultRoles);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultSettings(UUID tenantId) {
        List<Setting> settings = List.of(
                createSetting(tenantId, "general", "company_logo", "", "text", "Logo de la empresa"),
                createSetting(tenantId, "general", "business_hours", "{\"monday\":{\"start\":\"09:00\",\"end\":\"18:00\"},\"tuesday\":{\"start\":\"09:00\",\"end\":\"18:00\"},\"wednesday\":{\"start\":\"09:00\",\"end\":\"18:00\"},\"thursday\":{\"start\":\"09:00\",\"end\":\"18:00\"},\"friday\":{\"start\":\"09:00\",\"end\":\"18:00\"}}", "json", "Horario laboral"),
                createSetting(tenantId, "general", "timezone", "UTC", "text", "Zona horaria"),
                createSetting(tenantId, "general", "language", "es", "text", "Idioma por defecto"),
                createSetting(tenantId, "general", "currency", "USD", "text", "Moneda por defecto")
        );
        settingRepository.saveAll(settings);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultCategories(UUID tenantId) {
        List<Setting> categories = List.of(
                createSetting(tenantId, "product_category", "General", "Productos generales", "json"),
                createSetting(tenantId, "product_category", "Servicios", "Servicios profesionales", "json"),
                createSetting(tenantId, "product_category", "Digital", "Productos digitales", "json")
        );
        settingRepository.saveAll(categories);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultPipeline(UUID tenantId) {
        String pipelineJson = """
                {"name":"Pipeline por defecto","stages":[
                    {"name":"Nuevo","color":"#6366f1","order":0,"win_probability":10},
                    {"name":"Contactado","color":"#f59e0b","order":1,"win_probability":25},
                    {"name":"En negociación","color":"#3b82f6","order":2,"win_probability":50},
                    {"name":"Propuesta enviada","color":"#8b5cf6","order":3,"win_probability":75},
                    {"name":"Cerrado ganado","color":"#10b981","order":4,"win_probability":100},
                    {"name":"Cerrado perdido","color":"#ef4444","order":5,"win_probability":0}
                ]}
                """;
        Setting pipeline = createSetting(tenantId, "pipeline", "default", pipelineJson, "json", "Pipeline de ventas por defecto");
        settingRepository.save(pipeline);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultTags(UUID tenantId) {
        List<Setting> tags = List.of(
                createSetting(tenantId, "tag", "VIP", "Cliente VIP", "text"),
                createSetting(tenantId, "tag", "Nuevo", "Cliente nuevo", "text"),
                createSetting(tenantId, "tag", "Recurrente", "Cliente recurrente", "text"),
                createSetting(tenantId, "tag", "Lead caliente", "Lead con alta probabilidad", "text"),
                createSetting(tenantId, "tag", "Soporte", "Requiere soporte técnico", "text")
        );
        settingRepository.saveAll(tags);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultAiConfig(UUID tenantId) {
        Setting aiConfig = createSetting(tenantId, "ai", "provider", "NONE", "text", "Proveedor de IA");
        Setting aiPrompt = createSetting(tenantId, "ai", "system_prompt", "Eres un asistente virtual de atención al cliente amable y profesional. Responde en el mismo idioma que el cliente.", "text", "Prompt del sistema");
        Setting aiTemperature = createSetting(tenantId, "ai", "temperature", "0.7", "text", "Temperatura del modelo");
        Setting aiEnabled = createSetting(tenantId, "ai", "enabled", "false", "boolean", "IA habilitada");
        settingRepository.saveAll(List.of(aiConfig, aiPrompt, aiTemperature, aiEnabled));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultWhatsAppConfig(UUID tenantId) {
        List<Setting> whatsapp = List.of(
                createSetting(tenantId, "whatsapp", "provider", "EVOLUTION_API", "text", "Proveedor WhatsApp"),
                createSetting(tenantId, "whatsapp", "api_key", "", "text", "API Key"),
                createSetting(tenantId, "whatsapp", "phone_number", "", "text", "Número de teléfono"),
                createSetting(tenantId, "whatsapp", "webhook_url", "", "text", "URL del webhook"),
                createSetting(tenantId, "whatsapp", "instance_id", UUID.randomUUID().toString().substring(0, 8), "text", "ID de instancia"),
                createSetting(tenantId, "whatsapp", "connected", "false", "boolean", "Estado de conexión")
        );
        settingRepository.saveAll(whatsapp);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultNotificationPreferences(UUID tenantId) {
        String defaultPrefs = """
                {"new_message":true,"new_conversation":true,"conversation_assigned":true,
                 "lead_assigned":true,"lead_status_changed":true,"low_stock_alert":true,
                 "mention":true,"email_notifications":true,"push_notifications":false}
                """;
        Setting prefs = createSetting(tenantId, "notification", "preferences", defaultPrefs, "json", "Preferencias de notificación");
        settingRepository.save(prefs);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultSecurityConfig(UUID tenantId) {
        String securityJson = """
                {"max_login_attempts":5,"lockout_duration_minutes":15,
                 "password_min_length":8,"require_special_chars":true,
                 "session_timeout_minutes":480,"two_factor_enabled":false,
                 "allowed_ips":"","mfa_method":"none"}
                """;
        Setting security = createSetting(tenantId, "security", "config", securityJson, "json", "Configuración de seguridad");
        settingRepository.save(security);
    }

    private Role createRole(UUID tenantId, String name, String displayName, String description, boolean system) {
        return Role.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(name)
                .displayName(displayName)
                .description(description)
                .permissions("[]")
                .system(system)
                .build();
    }

    private Setting createSetting(UUID tenantId, String category, String key, String value, String type) {
        return Setting.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .category(category)
                .key(key)
                .value(value)
                .type(type)
                .build();
    }

    private Setting createSetting(UUID tenantId, String category, String key, String value, String type, String description) {
        return Setting.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .category(category)
                .key(key)
                .value(value)
                .type(type)
                .description(description)
                .build();
    }
}
