package com.iquenobot.chatbot.application;

import com.iquenobot.ai.domain.dto.AIMessageDto;
import com.iquenobot.chatbot.domain.dto.ChatbotPreviewRequestDto;
import com.iquenobot.chatbot.domain.dto.ChatbotPreviewResponseDto;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.chatbot.domain.dto.SimulatedActionDto;
import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.orchestrator.domain.model.BotConfiguration;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio del modo prueba del chatbot. Replica el flujo real de producción
 * para que el cliente vea exactamente cómo se comportaría el bot, pero sin
 * persistir nada en la base de datos ni ejecutar acciones reales (no crea
 * contactos, leads, cotizaciones PDF, no transfiere a agentes ni envía
 * mensajes por WhatsApp).
 *
 * <p>La configuración del bot (systemPrompt, fallbackMessage, temperature,
 * provider) se lee de los {@code settings} del tenant, igual que en producción,
 * de modo que la prueba refleja la configuración actual guardada.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotPreviewService {

    private final ChatbotService chatbotService;
    private final SettingRepository settingRepository;
    private final KnowledgeBaseService knowledgeBaseService;

    @Transactional
    public ChatbotPreviewResponseDto processPreviewMessage(ChatbotPreviewRequestDto request) {
        UUID tenantId = getTenantId();

        Map<String, Setting> settingsByKey = settingRepository
                .findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getCategory() + "." + s.getKey(),
                        s -> s,
                        (a, b) -> a));

        BotConfiguration botConfig = BotConfiguration.fromSettings(settingsByKey);

        if (!botConfig.isAiAvailable()) {
            log.info("Preview requested but bot is not available for tenant {}", tenantId);
            return ChatbotPreviewResponseDto.builder()
                    .botAvailable(false)
                    .message("El bot de IA no está habilitado para este negocio. "
                            + "Activa la IA y selecciona un proveedor en la configuración para probarlo.")
                    .simulatedActions(new ArrayList<>())
                    .build();
        }

        Map<String, Object> context = new HashMap<>();
        context.put("tenantId", tenantId.toString());
        context.put("channel", "PREVIEW");
        context.put("systemPrompt", botConfig.getSystemPrompt());
        context.put("fallbackMessage", botConfig.getFallbackMessage());
        context.put("temperature", String.valueOf(botConfig.getTemperature()));

        if (request.getContext() != null) {
            context.put("history", convertHistory(request.getContext().get("history")));
            context.put("isFirstMessage", request.getContext().getOrDefault("isFirstMessage", false));
            context.put("fallbackCount", request.getContext().getOrDefault("fallbackCount", 0));
        }

        ChatbotResponseDto response = chatbotService.processMessage(request.getMessage(), context, true);

        int fallbackCount = request.getContext() != null
                ? parseFallbackCount(request.getContext().get("fallbackCount"))
                : 0;

        return ChatbotPreviewResponseDto.builder()
                .message(response.getMessage())
                .intentDetected(response.getIntentDetected())
                .confidence(response.getConfidence())
                .requiresHumanAgent(response.isRequiresHumanAgent())
                .requiresClarification(response.isRequiresClarification())
                .flowExecuted(response.getFlowExecuted())
                .botAvailable(true)
                .simulatedActions(simulateActions(response, request.getMessage(), tenantId, fallbackCount))
                .build();
    }

    private int parseFallbackCount(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Deriva qué ejecutaría el orquestador en producción ante esta respuesta,
     * replicando la lógica de {@code BotDecisionStrategy} pero sin ejecutar nada.
     */
    private List<SimulatedActionDto> simulateActions(ChatbotResponseDto response, String message, UUID tenantId,
            int fallbackCount) {
        List<SimulatedActionDto> actions = new ArrayList<>();

        if (response.isRequiresHumanAgent()) {
            actions.add(action("TRANSFER_CONVERSATION", "Transferir a agente humano",
                    "La conversación se transferiría a un agente humano para su atención inmediata."));
            return actions;
        }

        if (response.isRequiresClarification()) {
            if (fallbackCount >= 1) {
                actions.add(action("TRANSFER_CONVERSATION", "Transferir a agente humano",
                        "El bot ya intentó aclarar la consulta antes y el cliente sigue sin ser entendido; "
                                + "en producción la conversación se transferiría a un agente humano."));
            } else {
                actions.add(action("SEND_TEXT", "Responder pidiendo aclaración",
                        "El bot enviaría un mensaje pidiendo al cliente que detalle su consulta. "
                                + "En producción, de no entenderse tras un segundo intento, se transferiría a un agente."));
            }
            return actions;
        }

        String intent = response.getIntentDetected();
        boolean purchaseIntent = message != null
                && ChatbotService.PURCHASE_INTENT_PATTERN.matcher(message.toLowerCase()).find();

        if (purchaseIntent) {
            actions.add(action("TRANSFER_CONVERSATION", "Transferir a agente para cerrar venta",
                    "El cliente muestra intención de compra; la conversación pasaría a un agente humano "
                            + "para concretar la venta."));
            return actions;
        }

        if (intent != null) {
            String leadTitle = commercialLeadTitle(intent);
            if (leadTitle != null) {
                actions.add(action("CREATE_LEAD", "Crear un lead (" + leadTitle + ")",
                        "Se registraría un lead comercial en el CRM con puntuación automática, "
                                + "para que puedas hacerle seguimiento."));
            }
            if ("solicitar_precio".equals(intent)) {
                List<Product> products = knowledgeBaseService.findMatchingProducts(tenantId, message);
                if (!products.isEmpty()) {
                    actions.add(action("SEND_QUOTE", "Enviar cotización PDF",
                            "Se generaría una cotización en PDF para " + products.size() + " producto(s) "
                                    + "y se enviaría por WhatsApp con los datos formales del cliente."));
                } else if (leadTitle == null) {
                    actions.add(action("CREATE_LEAD", "Crear un lead (Solicitud de cotización)",
                            "Se registraría un lead de cotización. No se encontraron productos coincidentes "
                                    + "para generar el PDF automáticamente."));
                }
            }
            if (!actions.isEmpty()) {
                return actions;
            }
        }

        actions.add(action("SEND_TEXT", "Responder por WhatsApp",
                "La respuesta del bot se enviaría al cliente por WhatsApp."));
        return actions;
    }

    private String commercialLeadTitle(String intent) {
        if ("solicitar_precio".equals(intent)) return "Solicitud de cotización";
        if ("consulta_envio".equals(intent)) return "Consulta de envío";
        if ("forma_pago".equals(intent)) return "Interés en compra - forma de pago";
        if ("informacion_empresa".equals(intent)) return "Interés general en productos/servicios";
        return null;
    }

    private SimulatedActionDto action(String type, String label, String description) {
        return SimulatedActionDto.builder()
                .actionType(type)
                .label(label)
                .description(description)
                .build();
    }

    /**
     * Convierte el historial recibido desde el frontend (lista de objetos
     * JSON) a una lista de {@code AIMessageDto}, que es la forma en que
     * {@code ChatbotService} espera el contexto en producción. Sin esta
     * conversión, el motor seguridad fallaría con ClassCastException al
     * tratar objetos {@code LinkedHashMap} como DTOs.
     */
    private List<AIMessageDto> convertHistory(Object rawHistory) {
        if (!(rawHistory instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<AIMessageDto> history = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof AIMessageDto dto) {
                history.add(dto);
            } else if (item instanceof Map<?, ?> map) {
                Object role = map.get("role");
                Object content = map.get("content");
                if (content != null && !content.toString().isBlank()) {
                    history.add(AIMessageDto.builder()
                            .role(role != null ? role.toString() : "user")
                            .content(content.toString())
                            .build());
                }
            }
        }
        return history;
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}