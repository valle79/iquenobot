package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.shared.enums.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotDecisionStrategyTest {

    @Mock
    private ChatbotService chatbotService;

    @InjectMocks
    private BotDecisionStrategy strategy;

    private Contact testContact;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        testContact = new Contact();
        testContact.setId(UUID.randomUUID());

        testConversation = new Conversation();
        testConversation.setId(UUID.randomUUID());
        testConversation.setBotConversation(true);
    }

    private ProcessingContext contextWithConversation(Conversation conversation) {
        ProcessingContext ctx = new ProcessingContext();
        ctx.setIncomingMessage(IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("5511999999999")
                .content("Hello")
                .build());
        ctx.setTenantId(UUID.randomUUID());
        ctx.setContact(testContact);
        ctx.setConversation(conversation);
        return ctx;
    }

    @Test
    void canHandle_shouldReturnTrue_whenBotConversation() {
        Conversation conversation = new Conversation();
        conversation.setBotConversation(true);

        assertTrue(strategy.canHandle(contextWithConversation(conversation)));
    }

    @Test
    void canHandle_shouldReturnTrue_whenNoAgentAssigned() {
        Conversation conversation = new Conversation();
        conversation.setBotConversation(false);
        conversation.setAssignedUser(null);

        assertTrue(strategy.canHandle(contextWithConversation(conversation)));
    }

    @Test
    void canHandle_shouldReturnFalse_whenAgentAssignedAndNotBotConversation() {
        Conversation conversation = new Conversation();
        conversation.setBotConversation(false);
        conversation.setAssignedUser(new com.iquenobot.auth.domain.entity.User());

        assertFalse(strategy.canHandle(contextWithConversation(conversation)));
    }

    @Test
    void decide_shouldReturnSendText_whenBotResponds() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setBotConversation(true);

        ProcessingContext ctx = contextWithConversation(conversation);

        when(chatbotService.processMessage(anyString(), anyMap())).thenReturn(
                ChatbotResponseDto.builder()
                        .message("Hi there!")
                        .intentDetected("greeting")
                        .flowExecuted("welcome")
                        .requiresHumanAgent(false)
                        .build()
        );

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.SEND_TEXT, decision.getActionType());
        assertEquals("Hi there!", decision.getParameters().get("response"));
        assertEquals("greeting", decision.getParameters().get("intent"));
    }

    @Test
    void decide_shouldReturnTransferConversation_whenBotRequestsHandoff() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setBotConversation(true);

        ProcessingContext ctx = contextWithConversation(conversation);

        when(chatbotService.processMessage(anyString(), anyMap())).thenReturn(
                ChatbotResponseDto.builder()
                        .message("I need help from a human")
                        .requiresHumanAgent(true)
                        .build()
        );

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.TRANSFER_CONVERSATION, decision.getActionType());
        assertTrue(decision.isRequiresAgent());
    }

    @Test
    void decide_shouldReturnTransferConversation_whenBotThrowsException() {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setBotConversation(true);

        ProcessingContext ctx = contextWithConversation(conversation);

        when(chatbotService.processMessage(anyString(), anyMap())).thenThrow(
                new RuntimeException("AI service unavailable")
        );

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.TRANSFER_CONVERSATION, decision.getActionType());
        assertTrue(decision.isRequiresAgent());
    }
}
