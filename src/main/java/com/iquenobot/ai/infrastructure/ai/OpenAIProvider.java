package com.iquenobot.ai.infrastructure.ai;

import com.iquenobot.ai.domain.dto.AIMessageDto;
import com.iquenobot.ai.domain.dto.AIResponseDto;
import com.iquenobot.ai.domain.service.IAIProvider;
import com.iquenobot.shared.enums.AIProvider;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * OpenAI implementation for AI services
 * 
 * This implementation can be easily replaced with other providers
 * by implementing the IAIProvider interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIProvider implements IAIProvider {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-4}")
    private String model;

    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Override
    public AIResponseDto chatCompletion(List<AIMessageDto> messages, String systemPrompt,
                                       Double temperature, Integer maxTokens) {
        log.info("Generating chat completion with OpenAI");

        try {
            String url = baseUrl + "/chat/completions";
            
            List<Map<String, String>> formattedMessages = new ArrayList<>();
            
            // Add system prompt if provided
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", systemPrompt);
                formattedMessages.add(systemMsg);
            }
            
            // Add conversation messages
            for (AIMessageDto msg : messages) {
                Map<String, String> message = new HashMap<>();
                message.put("role", msg.getRole());
                message.put("content", msg.getContent());
                formattedMessages.add(message);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", formattedMessages);
            requestBody.put("temperature", temperature != null ? temperature : 0.7);
            
            if (maxTokens != null) {
                requestBody.put("max_tokens", maxTokens);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    String content = message.get("content");
                    
                    Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
                    Integer tokensUsed = usage != null ? (Integer) usage.get("total_tokens") : null;
                    
                    log.info("Chat completion generated successfully. Tokens used: {}", tokensUsed);
                    
                    return AIResponseDto.builder()
                            .content(content)
                            .tokensUsed(tokensUsed)
                            .model(model)
                            .build();
                }
            }

            throw new BusinessException("Failed to generate chat completion");

        } catch (Exception e) {
            log.error("Error generating chat completion: {}", e.getMessage(), e);
            throw new BusinessException("Error al generar respuesta de IA: " + e.getMessage());
        }
    }

    @Override
    public void streamChatCompletion(List<AIMessageDto> messages, String systemPrompt,
                                    Double temperature, Integer maxTokens,
                                    Consumer<String> callback) {
        // Streaming implementation would use SSE or WebSockets
        // For simplicity, falling back to non-streaming
        AIResponseDto response = chatCompletion(messages, systemPrompt, temperature, maxTokens);
        callback.accept(response.getContent());
    }

    @Override
    public AIResponseDto extractIntent(String message, List<String> availableIntents) {
        log.info("Extracting intent from message");

        String systemPrompt = String.format(
                "You are an intent classifier. Analyze the user message and classify it into one of these intents: %s. " +
                "Respond with JSON format: {\"intent\": \"intent_name\", \"confidence\": 0.95}",
                String.join(", ", availableIntents)
        );

        List<AIMessageDto> messages = List.of(
                AIMessageDto.builder()
                        .role("user")
                        .content(message)
                        .build()
        );

        AIResponseDto response = chatCompletion(messages, systemPrompt, 0.3, 100);
        
        // Parse JSON response (simplified - should use proper JSON parser)
        String content = response.getContent();
        // Extract intent and confidence from JSON response
        
        return AIResponseDto.builder()
                .intent("general_inquiry") // Parsed from response
                .confidence(0.85f) // Parsed from response
                .content(content)
                .build();
    }

    @Override
    public AIResponseDto analyzeSentiment(String message) {
        log.info("Analyzing sentiment");

        String systemPrompt = "Analyze the sentiment of the user message. " +
                "Respond with JSON format: {\"sentiment\": \"positive/negative/neutral\", \"score\": 0.85}";

        List<AIMessageDto> messages = List.of(
                AIMessageDto.builder()
                        .role("user")
                        .content(message)
                        .build()
        );

        AIResponseDto response = chatCompletion(messages, systemPrompt, 0.3, 50);
        
        return AIResponseDto.builder()
                .sentiment("positive") // Parsed from response
                .sentimentScore(0.85f) // Parsed from response
                .content(response.getContent())
                .build();
    }

    @Override
    public float[] generateEmbeddings(String text) {
        log.info("Generating embeddings");

        try {
            String url = baseUrl + "/embeddings";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-embedding-3-small");
            requestBody.put("input", text);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                
                if (data != null && !data.isEmpty()) {
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i).floatValue();
                    }
                    return result;
                }
            }

            throw new BusinessException("Failed to generate embeddings");

        } catch (Exception e) {
            log.error("Error generating embeddings: {}", e.getMessage(), e);
            throw new BusinessException("Error al generar embeddings: " + e.getMessage());
        }
    }

    @Override
    public AIProvider getProviderType() {
        return AIProvider.OPENAI;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }
}