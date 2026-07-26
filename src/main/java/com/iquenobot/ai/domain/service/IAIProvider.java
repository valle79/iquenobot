package com.iquenobot.ai.domain.service;

import com.iquenobot.ai.domain.dto.AIMessageDto;
import com.iquenobot.ai.domain.dto.AIResponseDto;
import com.iquenobot.shared.enums.AIProvider;

import java.util.List;

/**
 * Interface for AI providers.
 * Implementations: OpenAI, Groq, Gemini, Claude
 * 
 * This interface decouples the system from any specific AI provider,
 * allowing easy switching between providers without changing business logic.
 */
public interface IAIProvider {

    /**
     * Generate a chat completion
     * @param messages Conversation history
     * @param systemPrompt System instructions
     * @param temperature Randomness (0.0 to 2.0)
     * @param maxTokens Maximum tokens in response
     * @return AI response
     */
    AIResponseDto chatCompletion(List<AIMessageDto> messages, String systemPrompt, 
                                 Double temperature, Integer maxTokens);

    /**
     * Generate a streaming chat completion
     * @param messages Conversation history
     * @param systemPrompt System instructions
     * @param temperature Randomness
     * @param maxTokens Maximum tokens
     * @param callback Callback for each token
     */
    void streamChatCompletion(List<AIMessageDto> messages, String systemPrompt,
                             Double temperature, Integer maxTokens,
                             java.util.function.Consumer<String> callback);

    /**
     * Extract intent from user message
     * @param message User message
     * @param availableIntents List of possible intents
     * @return Detected intent with confidence
     */
    AIResponseDto extractIntent(String message, List<String> availableIntents);

    /**
     * Analyze sentiment of a message
     * @param message Message to analyze
     * @return Sentiment analysis (positive, negative, neutral) with score
     */
    AIResponseDto analyzeSentiment(String message);

    /**
     * Generate embeddings for semantic search
     * @param text Text to embed
     * @return Embedding vector
     */
    float[] generateEmbeddings(String text);

    /**
     * Get provider type
     * @return Provider enum value
     */
    AIProvider getProviderType();

    /**
     * Check if provider is available
     * @return true if available, false otherwise
     */
    boolean isAvailable();
}