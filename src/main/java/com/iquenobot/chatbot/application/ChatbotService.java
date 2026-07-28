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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatbotFlowRepository flowRepository;
    private final ChatbotIntentRepository intentRepository;
    private final IAIProvider aiProvider;
    private final KnowledgeBaseService knowledgeBaseService;

    @Transactional
    public ChatbotResponseDto processMessage(String message, Map<String, Object> context) {
        UUID tenantId = getTenantId();
        
        log.info("Processing chatbot message for tenant: {}", tenantId);

        // 1. Try to detect intent
        ChatbotIntent intent = detectIntent(tenantId, message);
        
        // 2. Try to match a flow
        ChatbotFlow flow = matchFlow(tenantId, message, intent);
        
        // 3. Generate response
        if (flow != null) {
            return executeFlow(flow, message, context);
        } else if (intent != null) {
            return executeIntent(intent, message);
        } else {
            return generateAIResponse(message, context);
        }
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
    public ChatbotFlow matchFlow(UUID tenantId, String message, ChatbotIntent intent) {
        // 1. Try keyword trigger
        List<ChatbotFlow> flows = flowRepository.findByKeyword(tenantId, message);
        if (!flows.isEmpty()) {
            return flows.get(0);
        }
        
        // 2. Try intent trigger
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
            
            // In production, parse flowConfig JSON and execute steps
            return ChatbotResponseDto.builder()
                    .message(flow.getFallbackMessage() != null ? flow.getFallbackMessage() : "Procesando tu solicitud...")
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

    @Transactional
    public ChatbotResponseDto executeIntent(ChatbotIntent intent, String message) {
        log.info("Executing intent: {}", intent.getIntentName());
        
        // Parse responses and select one (in production, use more sophisticated logic)
        String[] responses = extractResponses(intent.getResponses());
        String selectedResponse = responses.length > 0 ? responses[0] : "¿En qué puedo ayudarte?";
        
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
        log.info("Generating AI response");
        
        try {
            String systemPrompt = (String) context.get("systemPrompt");
            String kbContext = knowledgeBaseService.buildContextForQuery(message);
            if (!kbContext.isEmpty()) {
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
        // Simple JSON parsing (in production, use proper JSON parser)
        if (json == null) return new String[0];
        return json.replace("[", "").replace("]", "")
                .replace("\"", "").split(",");
    }

    private String[] extractResponses(String json) {
        // Simple JSON parsing (in production, use proper JSON parser)
        if (json == null) return new String[0];
        return json.replace("[", "").replace("]", "")
                .replace("\"", "").split(",");
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
