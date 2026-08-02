package com.iquenobot.chatbot.application;

import com.iquenobot.ai.domain.dto.AIMessageDto;
import com.iquenobot.ai.domain.dto.AIResponseDto;
import com.iquenobot.ai.domain.service.IAIProvider;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.chatbot.domain.entity.ChatbotFlow;
import com.iquenobot.chatbot.domain.entity.ChatbotIntent;
import com.iquenobot.chatbot.domain.repository.ChatbotFlowRepository;
import com.iquenobot.chatbot.domain.repository.ChatbotIntentRepository;
import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ChatbotFlowTrigger;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatbotFlowRepository flowRepository;
    private final ChatbotIntentRepository intentRepository;
    private final IAIProvider aiProvider;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private static final java.util.regex.Pattern REQUEST_HUMAN_PATTERN = java.util.regex.Pattern.compile(
            "\\b(agente|representante|humano|asesor)\\b|hablar con (alguien|una persona|un agente|un representante)"
                    + "|(quiero|necesito|prefiero) (hablar|que me atienda) (con|un|una)");

    /** Frases que indican intención de compra inequívoca (el cliente confirma querer comprar). */
    public static final java.util.regex.Pattern PURCHASE_INTENT_PATTERN = java.util.regex.Pattern.compile(
            "\\b(comprar|compré|comprarlo|comprarla|comprarlos|adquirir|encargar|hacer (un|el|mi) pedido"
                    + "|orden de compra)\\b"
                    + "|(?<!no )(quiero (comprar|adquirir|encargar|pedir|hacer (un|el|mi) pedido"
                    + "|uno|una|este|esta|ese|esa))"
                    + "|(?<!no )me interesa (comprar|adquirir|hacer (un|el) pedido)");

    /**
     * Términos que indican una consulta comercial genérica ("¿qué venden?",
     * "¿qué ofrecen?", "catálogo", "precios"...). Aunque no exista contexto
     * previo ni match de KB, el bot responde con el LLM en lugar de pedir
     * aclaración, para no rechazar consultas legítimas de venta.
     */
    private static final String[] SALES_INQUIRY_TERMS = {
            "que venden", "q venden", "que vende", "q vende", "que vendes", "q vendes",
            "que ofrecen", "q ofrecen", "que ofrece", "q ofrece", "que ofreces",
            "que tienen", "q tienen", "que manejan", "q manejan", "que productos",
            "catalogo", "catálogo", "sus productos", "tus productos", "los productos",
            "servicios que", "informacion de productos", "información de productos",
            "informacion de sus productos", "información de sus productos",
            "precios", "precio de", "cuanto cuesta", "cuánto cuesta", "cuanto vale", "cuánto vale",
            "que es lo que venden", "q es lo que venden", "a que se dedican", "a qué se dedican",
            "que vende la empresa", "q vende la empresa", "en que trabajan", "en qué trabajan",
            "productos que venden", "productos que ofrecen", "que tipo de productos",
            "q tipo de productos", "que oferta", "q oferta"
    };

    private boolean isSalesInquiry(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        for (String term : SALES_INQUIRY_TERMS) {
            if (normalized.contains(term)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public ChatbotResponseDto processMessage(String message, Map<String, Object> context) {
        UUID tenantId = getTenantId();
        
        log.info("Processing chatbot message for tenant: {}", tenantId);

        // 1. Try to detect intent
        ChatbotIntent intent = detectIntent(tenantId, message);
        
        // 2. Try to match a flow
        ChatbotFlow flow = matchFlow(tenantId, message, intent, context);
        
        // 3. Generate response
        if (flow != null) {
            return executeFlow(flow, message, context);
        } else if (intent != null) {
            return executeIntent(intent, message);
        } else {
            return handleFallback(message, context);
        }
    }

    /**
     * Respuesta de respaldo cuando no hay intent ni flow configurado.
     *
     * Si existe conocimiento relevante en la KB, en el catálogo de productos o
     * historial previo de la conversación, el bot responde con el LLM usando ese
     * contexto (esto permite interpretar referencias como "la opción 4" o
     * acuses como "Interesante" tras una respuesta del bot). De lo contrario NO
     * responde con el LLM (evita respuestas inventadas a mensajes fuera de
     * alcance o personales), sino que pide al cliente que detalle su consulta.
     * La transferencia al agente humano solo se produce si el cliente la solicita
     * explícitamente o si el orquestador agota los intentos de aclaración.
     */
    private ChatbotResponseDto handleFallback(String message, Map<String, Object> context) {
        boolean requestsHuman = REQUEST_HUMAN_PATTERN.matcher(message.toLowerCase()).find();
        if (requestsHuman) {
            log.info("Customer explicitly requested a human agent; transferring");
            return ChatbotResponseDto.builder()
                    .message(resolveFallbackMessage(context))
                    .requiresHumanAgent(true)
                    .requiresClarification(false)
                    .suggestedActions(new ArrayList<>())
                    .entities(new HashMap<>())
                    .build();
        }

        @SuppressWarnings("unchecked")
        List<AIMessageDto> history = (List<AIMessageDto>) context.getOrDefault("history", new ArrayList<>());
        boolean hasConversationContext = history.stream().anyMatch(m -> "assistant".equals(m.getRole()));

        // Las consultas comerciales genéricas ("¿qué venden?", "catálogo", "precios"...)
        // SIEMPRE se responden con el LLM (con el contexto de KB/productos del tenant
        // si existe), para no rechazar consultas legítimas de venta con "no entendí".
        String kbContext = knowledgeBaseService.buildContextForQuery(buildContextQuery(message, history));
        if (hasConversationContext || !kbContext.isEmpty() || isSalesInquiry(message)) {
            return generateAIResponse(message, context, kbContext);
        }

        log.info("No intent, flow, KB or product context matched; asking customer to clarify");
        return ChatbotResponseDto.builder()
                .message(resolveFallbackMessage(context))
                .requiresHumanAgent(false)
                .requiresClarification(true)
                .suggestedActions(new ArrayList<>())
                .entities(new HashMap<>())
                .build();
    }

    private String resolveFallbackMessage(Map<String, Object> context) {
        return context.get("fallbackMessage") != null
                ? (String) context.get("fallbackMessage")
                : "Gracias por escribirnos. Disculpa, no logré entender completamente tu consulta. "
                        + "Para poder ayudarte de la mejor manera, ¿podrías indicarme con mayor detalle qué es "
                        + "exactamente lo que deseas? Por ejemplo: el nombre del producto, el modelo o la "
                        + "información que necesitas. Un agente humano está al tanto de tu mensaje y te "
                        + "atenderá muy pronto. ¡Gracias por tu paciencia!";
    }

    /**
     * Combina el mensaje actual con los últimos mensajes del bot para que la
     * búsqueda en la base de conocimiento y el catálogo pueda resolver
     * referencias (p. ej. "la opción 4" o "ese modelo") contra lo que el bot
     * mostró antes en la conversación.
     */
    private String buildContextQuery(String message, List<AIMessageDto> history) {
        StringBuilder sb = new StringBuilder(message == null ? "" : message);
        int appended = 0;
        for (int i = history.size() - 1; i >= 0 && appended < 2; i--) {
            AIMessageDto m = history.get(i);
            if ("assistant".equals(m.getRole()) && m.getContent() != null && !m.getContent().isBlank()) {
                sb.append(' ').append(m.getContent());
                appended++;
            }
        }
        return sb.length() > 1000 ? sb.substring(0, 1000) : sb.toString();
    }

    @Transactional(readOnly = true)
    public ChatbotIntent detectIntent(UUID tenantId, String message) {
        List<ChatbotIntent> activeIntents = intentRepository.findByTenantIdAndActiveAndDeletedFalseOrderByPriorityDesc(
                tenantId, true);
        
        for (ChatbotIntent intent : activeIntents) {
            // Simple keyword matching (in production, use NLP/ML)
            String[] keywords = extractKeywords(intent.getTrainingPhrases());
            for (String keyword : keywords) {
                if (message.toLowerCase().contains(keyword.toLowerCase())) {
                    intent.incrementMatchedCount();
                    intentRepository.save(intent);
                    return intent;
                }
            }
        }
        
        return null;
    }

    @Transactional(readOnly = true)
    public ChatbotFlow matchFlow(UUID tenantId, String message, ChatbotIntent intent, Map<String, Object> context) {
        // 1. Try keyword trigger
        List<ChatbotFlow> flows = flowRepository.findByKeyword(tenantId, message);
        if (!flows.isEmpty()) {
            return flows.get(0);
        }

        // 2. Try WELCOME trigger (first message in conversation)
        if (context != null && Boolean.TRUE.equals(context.get("isFirstMessage"))) {
            flows = flowRepository.findByTenantIdAndTriggerTypeAndActiveAndDeletedFalse(
                    tenantId, ChatbotFlowTrigger.WELCOME, true);
            if (!flows.isEmpty()) {
                return flows.get(0);
            }
        }

        // 3. Try intent trigger
        if (intent != null) {
            flows = flowRepository.findByTenantIdAndTriggerTypeAndActiveAndDeletedFalse(
                    tenantId, ChatbotFlowTrigger.INTENT, true);
            if (!flows.isEmpty()) {
                return flows.get(0);
            }
        }

        return null;
    }

    @Transactional
    public ChatbotResponseDto executeFlow(ChatbotFlow flow, String message, Map<String, Object> context) {
        log.info("Executing flow: {}", flow.getName());
        
        try {
            flow.incrementSuccess();
            flowRepository.save(flow);
            
            String botMessage = extractFlowMessage(flow.getFlowConfig());
            if (botMessage == null) {
                botMessage = flow.getFallbackMessage() != null ? flow.getFallbackMessage() : "Procesando tu solicitud...";
            }
            
            return ChatbotResponseDto.builder()
                    .message(botMessage)
                    .flowExecuted(flow.getName())
                    .requiresHumanAgent(false)
                    .suggestedActions(new ArrayList<>())
                    .entities(new HashMap<>())
                    .build();
        } catch (Exception e) {
            flow.incrementFailure();
            flowRepository.save(flow);
            throw e;
        }
    }

    private String extractFlowMessage(String flowConfig) {
        if (flowConfig == null || flowConfig.isBlank()) return null;
        try {
            var node = objectMapper.readTree(flowConfig);
            var msg = node.get("message");
            return msg != null ? msg.asText() : null;
        } catch (JsonProcessingException e) {
            log.warn("Invalid flowConfig JSON: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public ChatbotResponseDto executeIntent(ChatbotIntent intent, String message) {
        log.info("Executing intent: {}", intent.getIntentName());
        
        String[] responses = extractResponses(intent.getResponses());
        String selectedResponse = responses.length > 0
                ? responses[random.nextInt(responses.length)]
                : "¿En qué puedo ayudarte?";
        
        return ChatbotResponseDto.builder()
                .message(selectedResponse)
                .intentDetected(intent.getIntentName())
                .confidence(intent.getConfidenceThreshold())
                .requiresHumanAgent(false)
                .suggestedActions(new ArrayList<>())
                .entities(new HashMap<>())
                .build();
    }

    @Transactional
    public ChatbotResponseDto generateAIResponse(String message, Map<String, Object> context) {
        String kbContext = context.get("kbContext") != null
                ? (String) context.get("kbContext")
                : knowledgeBaseService.buildContextForQuery(message);
        return generateAIResponse(message, context, kbContext);
    }

    @Transactional
    public ChatbotResponseDto generateAIResponse(String message, Map<String, Object> context, String kbContext) {
        log.info("Generating AI response");

        try {
            String systemPrompt = (String) context.get("systemPrompt");

            // El bot siempre se alimenta del módulo de conocimientos: los documentos
            // de comportamiento del tenant (reglas, tono, procedimientos) definen cómo
            // debe portarse y qué debe realizar en cada respuesta.
            String behaviorContext = knowledgeBaseService.buildBehaviorContext();
            if (behaviorContext != null && !behaviorContext.isEmpty()) {
                systemPrompt = (systemPrompt != null ? systemPrompt : "") + behaviorContext;
            }

            if (kbContext != null && !kbContext.isEmpty()) {
                systemPrompt = (systemPrompt != null ? systemPrompt : "") + kbContext;
            }

            AIMessageDto aiMessage = AIMessageDto.builder()
                    .role("user")
                    .content(message)
                    .build();
            
            List<AIMessageDto> history = (List<AIMessageDto>) context.getOrDefault("history", new ArrayList<>());
            history.add(aiMessage);
            
            Double temperature = context.get("temperature") instanceof String t ? Double.parseDouble(t) : null;
            AIResponseDto aiResponse = aiProvider.chatCompletion(history, systemPrompt, temperature, null);
            
            return ChatbotResponseDto.builder()
                    .message(aiResponse.getContent())
                    .confidence(0.0)
                    .requiresHumanAgent(false)
                    .suggestedActions(new ArrayList<>())
                    .entities(new HashMap<>())
                    .build();
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return ChatbotResponseDto.builder()
                    .message("Lo siento, no pude procesar tu mensaje. ¿Puedo conectarte con un agente?")
                    .requiresHumanAgent(true)
                    .build();
        }
    }

    private String[] extractKeywords(String json) {
        if (json == null || json.isBlank()) return new String[0];
        try {
            var arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                String[] result = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    result[i] = arr.get(i).asText();
                }
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid keywords JSON: {}", e.getMessage());
        }
        return new String[0];
    }

    private String[] extractResponses(String json) {
        if (json == null || json.isBlank()) return new String[0];
        try {
            var arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                String[] result = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    result[i] = arr.get(i).asText();
                }
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("Invalid responses JSON: {}", e.getMessage());
        }
        return new String[0];
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
