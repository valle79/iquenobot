package com.iquenobot.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.chatbot.domain.entity.ChatbotFlow;
import com.iquenobot.chatbot.domain.entity.ChatbotIntent;
import com.iquenobot.chatbot.domain.repository.ChatbotFlowRepository;
import com.iquenobot.chatbot.domain.repository.ChatbotIntentRepository;
import com.iquenobot.channel.domain.entity.WhatsAppChannel;
import com.iquenobot.channel.domain.repository.WhatsAppChannelRepository;
import com.iquenobot.role.domain.entity.Role;
import com.iquenobot.role.domain.repository.RoleRepository;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.enums.ChatbotFlowTrigger;
import com.iquenobot.shared.enums.WhatsAppChannelStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultDataSeeder {

    private final RoleRepository roleRepository;
    private final SettingRepository settingRepository;
    private final WhatsAppChannelRepository whatsAppChannelRepository;
    private final ChatbotIntentRepository chatbotIntentRepository;
    private final ChatbotFlowRepository chatbotFlowRepository;
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
        String instanceId = UUID.randomUUID().toString().substring(0, 8);
        List<Setting> whatsapp = List.of(
                createSetting(tenantId, "whatsapp", "provider", "EVOLUTION_API", "text", "Proveedor WhatsApp"),
                createSetting(tenantId, "whatsapp", "api_key", "", "text", "API Key"),
                createSetting(tenantId, "whatsapp", "phone_number", "", "text", "Número de teléfono"),
                createSetting(tenantId, "whatsapp", "webhook_url", "", "text", "URL del webhook"),
                createSetting(tenantId, "whatsapp", "instance_id", instanceId, "text", "ID de instancia"),
                createSetting(tenantId, "whatsapp", "connected", "false", "boolean", "Estado de conexión")
        );
        settingRepository.saveAll(whatsapp);

        if (!whatsAppChannelRepository.existsByInstanceNameAndDeletedFalse(instanceId)
                && whatsAppChannelRepository.countByTenantIdAndDeletedFalse(tenantId) == 0) {
            WhatsAppChannel channel = WhatsAppChannel.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .channelName("WhatsApp Principal")
                    .instanceName(instanceId)
                    .status(WhatsAppChannelStatus.DISCONNECTED)
                    .build();
            whatsAppChannelRepository.save(channel);
        }
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

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultChatbotIntents(UUID tenantId) {
        List<ChatbotIntent> intents = List.of(
                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("saludo")
                        .description("Saludos cordiales de bienvenida")
                        .trainingPhrases(jsonArray("Hola", "Buenos días", "Buenas tardes", "Buenas noches", "Qué tal", "Hey", "Hola, cómo estás", "Buen día"))
                        .responses(jsonArray(
                                "¡Hola! ¿En qué puedo ayudarte el día de hoy? Estoy aquí para servirte.",
                                "¡Buenos días! Bienvenido, ¿cómo puedo asistirte?",
                                "¡Hola! Un gusto saludarte. ¿En qué puedo servirte?"
                        ))
                        .confidenceThreshold(0.7).active(true).priority(1).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("consulta_horario")
                        .description("Consultas sobre horarios de atención")
                        .trainingPhrases(jsonArray("horario", "horario de atención", "a qué hora abren", "a qué hora cierran", "atienden sábados", "atienden domingos", "fines de semana", "hasta qué hora atienden", "días de atención"))
                        .responses(jsonArray(
                                "Nuestro horario de atención es de lunes a viernes de 9:00 a.m. a 6:00 p.m. y los sábados de 9:00 a.m. a 1:00 p.m. ¿En qué más puedo ayudarte?",
                                "Atendemos de lunes a viernes de 9am a 6pm, y sábados de 9am a 1pm. Domingo cerrado. ¿Algo más en que pueda servirte?"
                        ))
                        .confidenceThreshold(0.7).active(true).priority(2).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("solicitar_precio")
                        .description("Consultas sobre precios y cotizaciones")
                        .trainingPhrases(jsonArray("precio", "costo", "cuánto vale", "cuánto cuesta", "tarifa", "presupuesto", "cotización", "quiero comprar", "me interesa"))
                        .responses(jsonArray(
                                "Gracias por tu interés. Permíteme consultar los precios actualizados para brindarte la mejor información. ¿Podrías indicarme qué producto o servicio te interesa?",
                                "Con gusto te ayudo con los precios. ¿Qué producto o servicio deseas consultar? Así puedo darte una cotización personalizada."
                        ))
                        .confidenceThreshold(0.7).active(true).priority(3).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("ubicacion")
                        .description("Consultas sobre dirección y ubicación")
                        .trainingPhrases(jsonArray("dirección", "ubicación", "dónde están", "cómo llegar", "mapa", "oficina", "local", "dónde queda"))
                        .responses(jsonArray(
                                "Nos encontramos en [Dirección de la empresa]. ¿Deseas que te envíe la ubicación por Google Maps?",
                                "Puedes visitarnos en [Dirección de la empresa]. Estaremos encantados de atenderte en nuestro local."
                        ))
                        .confidenceThreshold(0.7).active(true).priority(4).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("contactar_asesor")
                        .description("Solicitud de contacto con un agente humano")
                        .trainingPhrases(jsonArray("agente", "asesor", "hablar con alguien", "persona", "operador", "atención al cliente", "ejecutivo", "representante", "transferir"))
                        .responses(jsonArray(
                                "Por supuesto, te conectamos con un asesor en este momento. Por favor, espera un instante.",
                                "Claro, un asesor se comunicará contigo a la brevedad. Gracias por tu paciencia.",
                                "Entiendo, en un momento te atenderá un agente. Por favor, mantente en línea."
                        ))
                        .confidenceThreshold(0.7).active(true).priority(5).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("consulta_envio")
                        .description("Consultas sobre envíos y entregas")
                        .trainingPhrases(jsonArray("envío", "delivery", "despacho", "entrega", "cuándo llega", "tiempo de entrega", "demora", "seguimiento de pedido", "rastrear"))
                        .responses(jsonArray(
                                "El tiempo de entrega estimado es de 2 a 5 días hábiles dentro de Lima y de 5 a 7 días hábiles para provincias. ¿Te gustaría rastrear tu pedido?",
                                "Realizamos envíos a todo el Perú. El costo y tiempo de entrega varían según tu ubicación. ¿Podrías indicarme tu distrito o ciudad para darte mayor precisión?"
                        ))
                        .confidenceThreshold(0.7).active(true).priority(6).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("forma_pago")
                        .description("Consultas sobre métodos de pago")
                        .trainingPhrases(jsonArray("pago", "pagar", "tarjeta", "transferencia", "yape", "plin", "efectivo", "depósito", "métodos de pago"))
                        .responses(jsonArray(
                                "Aceptamos pagos con tarjeta de crédito/débito, transferencia bancaria, Yape y Plin. ¿Cuál es tu método de pago preferido?",
                                "Puedes pagar con tarjeta (Visa, Mastercard), transferencia bancaria, Yape o Plin. ¿Cuál te resulta más conveniente?"
                        ))
                        .confidenceThreshold(0.7).active(true).priority(7).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("queja_reclamo")
                        .description("Atención de quejas o reclamos")
                        .trainingPhrases(jsonArray("queja", "reclamo", "problema", "insatisfecho", "error", "mal servicio", "devolución", "me quejo"))
                        .responses(jsonArray(
                                "Lamento mucho el inconveniente. Permíteme revisar tu caso para darte una solución rápida. ¿Podrías contarme un poco más sobre lo sucedido?",
                                "Agradecemos tu feedback, ya que nos ayuda a mejorar. Un asesor especializado revisará tu caso y te contactará a la brevedad.",
                                "Lamento el problema que has experimentado. Déjame tus datos y un supervisor se comunicará contigo en los próximos minutos para resolverlo."
                        ))
                        .confidenceThreshold(0.7).active(true).priority(8).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("informacion_empresa")
                        .description("Solicitud de información general de la empresa")
                        .trainingPhrases(jsonArray("quiénes son", "empresa", "acerca de", "información", "quién es", "sobre ustedes", "qué hacen"))
                        .responses(jsonArray(
                                "Somos una empresa comprometida con brindar el mejor servicio a nuestros clientes. ¿Hay algo específico que te gustaría saber sobre nosotros? Con gusto te informamos.",
                                "Con gusto te brindamos información sobre nuestra empresa. ¿Hay algo en particular que te gustaría conocer?"
                        ))
                        .confidenceThreshold(0.7).active(true).priority(9).matchedCount(0L).build(),

                ChatbotIntent.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .intentName("despedida")
                        .description("Despedidas cordiales")
                        .trainingPhrases(jsonArray("gracias", "adiós", "chau", "hasta luego", "nos vemos", "que tengas buen día", "muchas gracias", "gracias por tu atención"))
                        .responses(jsonArray(
                                "Gracias a ti por contactarnos. ¡Que tengas un excelente día! Si necesitas algo más, aquí estamos.",
                                "Ha sido un placer atenderte. No dudes en escribirnos si requieres algo más. ¡Hasta pronto!",
                                "¡Gracias por comunicarte! Quedo atento por si surge cualquier otra consulta. Que tengas un maravilloso día."
                        ))
                        .confidenceThreshold(0.7).active(true).priority(10).matchedCount(0L).build()
        );
        chatbotIntentRepository.saveAll(intents);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void seedDefaultChatbotFlows(UUID tenantId) {
        List<ChatbotFlow> flows = List.of(
                ChatbotFlow.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .name("Bienvenida")
                        .description("Saludo inicial cuando un cliente escribe por primera vez")
                        .triggerType(ChatbotFlowTrigger.WELCOME)
                        .flowConfig("{\"message\":\"¡Hola! Soy el asistente virtual de la empresa. Estoy aquí para ayudarte. Puedes consultarme sobre horarios, precios, productos, o si prefieres, puedo comunicarte con un asesor. ¿En qué puedo servirte el día de hoy?\"}")
                        .active(true).priority(1)
                        .successCount(0L).failureCount(0L).executionCount(0L).build(),

                ChatbotFlow.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .name("Consulta de precios")
                        .description("Guía al cliente cuando pregunta por precios")
                        .triggerType(ChatbotFlowTrigger.KEYWORD)
                        .triggerKeywords("precio, costo, cuánto vale, tarifa, cotización")
                        .flowConfig("{\"message\":\"Entiendo que deseas información sobre precios. Permíteme ayudarte con eso. ¿Podrías indicarme exactamente qué producto o servicio te interesa? Así puedo brindarte una cotización precisa y personalizada.\"}")
                        .fallbackMessage("Gracias por tu consulta. Un asesor se comunicará contigo para darte los precios actualizados.")
                        .active(true).priority(2)
                        .successCount(0L).failureCount(0L).executionCount(0L).build(),

                ChatbotFlow.builder()
                        .id(UUID.randomUUID()).tenantId(tenantId)
                        .name("Soporte técnico")
                        .description("Deriva a soporte técnico cuando el cliente reporta un problema")
                        .triggerType(ChatbotFlowTrigger.KEYWORD)
                        .triggerKeywords("soporte, ayuda técnica, problema técnico, falla, error, no funciona")
                        .flowConfig("{\"message\":\"Lamento que estés experimentando dificultades. Permíteme tomar nota de tu caso para que un especialista en soporte técnico te contacte a la mayor brevedad. Por favor, cuéntame brevemente cuál es el problema que estás presentando.\"}")
                        .fallbackMessage("Gracias por reportarlo. Un técnico especializado revisará tu caso y te contactará pronto.")
                        .active(true).priority(3)
                        .successCount(0L).failureCount(0L).executionCount(0L).build()
        );
        chatbotFlowRepository.saveAll(flows);
    }

    private String jsonArray(String... values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create JSON array", e);
        }
    }
}
