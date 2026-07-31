package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.orchestrator.domain.model.ConversationContext;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConversationContextResolutionStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ConversationMessageRepository messageRepository;

    @Override
    public int getOrder() { return 37; }

    @Override
    public ProcessingContext execute(ProcessingContext context) {
        if (context.getConversation() == null) {
            context.setConversationContext(ConversationContext.empty());
            return context;
        }

        var conversation = context.getConversation();

        List<ConversationMessage> recentMessages = messageRepository
                .findByConversationIdOrderBySentAtDesc(conversation.getId(), PageRequest.of(0, 20))
                .getContent();

        int totalMessages = conversation.getMessageCount() != null ? conversation.getMessageCount() : 0;
        int messagesFromBot = 0;
        int messagesFromContact = 0;
        int consecutiveBotResponses = 0;
        LocalDateTime lastBotResponseAt = null;
        LocalDateTime lastContactMessageAt = null;
        List<String> recentIntents = new ArrayList<>();

        for (ConversationMessage msg : recentMessages) {
            if (msg.isFromBot()) {
                messagesFromBot++;
                if (lastBotResponseAt == null) {
                    lastBotResponseAt = msg.getSentAt();
                }
            } else if (msg.isInbound()) {
                messagesFromContact++;
                if (lastContactMessageAt == null) {
                    lastContactMessageAt = msg.getSentAt();
                }
            }
        }

        for (ConversationMessage msg : recentMessages) {
            if (msg.isFromBot() && msg.getBotIntent() != null && !msg.getBotIntent().isBlank()) {
                recentIntents.add(msg.getBotIntent());
                if (recentIntents.size() >= 5) break;
            }
        }

        for (ConversationMessage msg : recentMessages) {
            if (msg.isFromBot()) {
                consecutiveBotResponses++;
            } else {
                break;
            }
        }

        long secondsSinceLastMessage = 0;
        if (lastContactMessageAt != null) {
            secondsSinceLastMessage = Duration.between(lastContactMessageAt, LocalDateTime.now(ZoneOffset.UTC)).getSeconds();
        }

        boolean firstMessage = totalMessages <= 1;

        ConversationContext conversationContext = ConversationContext.builder()
                .totalMessages(totalMessages)
                .messagesFromContact(messagesFromContact)
                .messagesFromBot(messagesFromBot)
                .lastBotResponseAt(lastBotResponseAt)
                .lastContactMessageAt(lastContactMessageAt)
                .recentIntents(recentIntents)
                .firstMessageInConversation(firstMessage)
                .secondsSinceLastMessage(secondsSinceLastMessage)
                .consecutiveBotResponses(consecutiveBotResponses)
                .build();

        context.setConversationContext(conversationContext);

        log.debug("Conversation context resolved: totalMessages={} consecutiveBotResponses={} firstMessage={}",
                totalMessages, consecutiveBotResponses, firstMessage);
        return context;
    }
}
