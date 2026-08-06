package com.iquenobot.orchestrator.application.decision;

import com.iquenobot.chatbot.application.ChatbotService;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.application.ConversationHandoffService;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.BotConfiguration;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.shared.enums.ChannelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BotDecisionStrategyTest {

    @Mock
    private ChatbotService chatbotService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationMessageRepository messageRepository;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private ConversationHandoffService handoffService;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private BotDecisionStrategy strategy;

    private Contact testContact;
    private Conversation testConversation;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testContact = new Contact();
        testContact.setId(UUID.randomUUID());
        testContact.setFirstName("Juan");
        testContact.setFullName("Juan Pérez");

        testConversation = new Conversation();
        testConversation.setId(UUID.randomUUID());
        testConversation.setBotConversation(true);
        testConversation.setMessageCount(1);

        testProduct = new Product();
        testProduct.setId(UUID.randomUUID());
    }

    private ProcessingContext contextWithConversation(Conversation conversation, String content) {
        ProcessingContext ctx = new ProcessingContext();
        ctx.setIncomingMessage(IncomingMessage.builder()
                .channel(ChannelType.WHATSAPP)
                .sourceIdentifier("5511999999999")
                .content(content)
                .build());
        ctx.setTenantId(UUID.randomUUID());
        ctx.setContact(testContact);
        ctx.setConversation(conversation);
        ctx.setBotConfiguration(BotConfiguration.builder().enabled(true).aiProvider("GROQ").build());
        return ctx;
    }

    private void stubHistory() {
        when(messageRepository.findByConversationIdOrderBySentAtDesc(any(), any()))
                .thenReturn(Page.empty());
    }

    @Test
    void canHandle_shouldReturnTrue_whenBotAvailable() {
        Conversation conversation = new Conversation();
        conversation.setBotConversation(true);
        when(handoffService.canBotRespond(any(Conversation.class))).thenReturn(true);

        assertTrue(strategy.canHandle(contextWithConversation(conversation, "Hello")));
    }

    @Test
    void canHandle_shouldReturnTrue_whenNoAgentAssigned() {
        Conversation conversation = new Conversation();
        conversation.setBotConversation(false);
        conversation.setAssignedUser(null);
        when(handoffService.canBotRespond(any(Conversation.class))).thenReturn(true);

        assertTrue(strategy.canHandle(contextWithConversation(conversation, "Hello")));
    }

    @Test
    void canHandle_shouldReturnFalse_whenAgentBlocksBot() {
        Conversation conversation = new Conversation();
        conversation.setBotConversation(false);
        conversation.setAssignedUser(new com.iquenobot.auth.domain.entity.User());
        when(handoffService.canBotRespond(any(Conversation.class))).thenReturn(false);

        assertFalse(strategy.canHandle(contextWithConversation(conversation, "Hello")));
    }

    @Test
    void decide_shouldReturnSendText_whenBotResponds() {
        ProcessingContext ctx = contextWithConversation(testConversation, "Hello");
        stubHistory();

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
        ProcessingContext ctx = contextWithConversation(testConversation, "I need a human");
        stubHistory();

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
        ProcessingContext ctx = contextWithConversation(testConversation, "Hello");
        stubHistory();

        when(chatbotService.processMessage(anyString(), anyMap())).thenThrow(
                new RuntimeException("AI service unavailable")
        );

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.TRANSFER_CONVERSATION, decision.getActionType());
        assertTrue(decision.isRequiresAgent());
    }

    @Test
    void decide_shouldAskForBillingData_whenQuoteRequestedAndContactIncomplete() {
        testContact.setFirstName(null);
        testContact.setLastName(null);
        testContact.setFullName(null);
        ProcessingContext ctx = contextWithConversation(testConversation, "quiero la cotización de la cosechadora");
        stubHistory();
        when(chatbotService.processMessage(anyString(), anyMap())).thenReturn(
                ChatbotResponseDto.builder()
                        .message("Con gusto te ayudo")
                        .intentDetected("solicitar_precio")
                        .requiresHumanAgent(false)
                        .build()
        );
        when(knowledgeBaseService.findMatchingProducts(eq(ctx.getTenantId()), anyString()))
                .thenReturn(List.of(testProduct));

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.SEND_TEXT, decision.getActionType());
        String response = (String) decision.getParameters().get("response");
        assertNotNull(response);
        assertTrue(response.contains("DNI o RUC"), "El bot debe pedir el documento fiscal");
        assertTrue(response.contains("nombre completo"), "El bot debe pedir el nombre completo");
        assertTrue(testConversation.getMetadata().contains(testProduct.getId().toString()),
                "La cotización debe quedar pendiente con los productos");
        verify(conversationRepository).save(testConversation);
    }

    @Test
    void decide_shouldSendQuote_whenQuoteRequestedAndContactComplete() {
        testContact.setDocumentNumber("12345678");
        testContact.setDocumentType("DNI");
        ProcessingContext ctx = contextWithConversation(testConversation, "quiero la cotización de la cosechadora");
        stubHistory();
        when(chatbotService.processMessage(anyString(), anyMap())).thenReturn(
                ChatbotResponseDto.builder()
                        .message("Con gusto te ayudo")
                        .intentDetected("solicitar_precio")
                        .requiresHumanAgent(false)
                        .build()
        );
        when(knowledgeBaseService.findMatchingProducts(eq(ctx.getTenantId()), anyString()))
                .thenReturn(List.of(testProduct));

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.SEND_QUOTE, decision.getActionType());
        String productIds = (String) decision.getParameters().get("productIds");
        assertTrue(productIds.contains(testProduct.getId().toString()));
    }

    @Test
    void decide_shouldSendPendingQuote_whenContactDataArrives() {
        String pendingId = UUID.randomUUID().toString();
        testConversation.setMetadata("{\"pending_quote_product_ids\":[\"" + pendingId + "\"]}");
        testContact.setDocumentNumber("20512345678");
        testContact.setCompany("Agro Test SAC");
        ProcessingContext ctx = contextWithConversation(testConversation, "mi RUC es 20512345678");
        stubHistory();
        when(chatbotService.processMessage(anyString(), anyMap())).thenReturn(
                ChatbotResponseDto.builder()
                        .message("Gracias por tus datos")
                        .intentDetected("unknown")
                        .requiresHumanAgent(false)
                        .build()
        );
        when(contactRepository.findByIdAndTenantIdAndDeletedFalse(eq(ctx.getContact().getId()), eq(ctx.getTenantId())))
                .thenReturn(Optional.of(testContact));

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.SEND_QUOTE, decision.getActionType());
        String productIds = (String) decision.getParameters().get("productIds");
        assertTrue(productIds.contains(pendingId));
        assertFalse(testConversation.getMetadata().contains(pendingId),
                "La cotización pendiente debe limpiarse de la metadata");
    }

    @Test
    void decide_shouldClearExpiredPendingQuote_andNotAskForBillingData() {
        String pendingId = UUID.randomUUID().toString();
        long expiredAt = System.currentTimeMillis() - (61 * 60 * 1000L);
        testConversation.setMetadata("{\"pending_quote_product_ids\":[\"" + pendingId
                + "\"],\"pending_quote_created_at\":" + expiredAt + "}");
        ProcessingContext ctx = contextWithConversation(testConversation, "¿tienen stock?");
        stubHistory();
        when(chatbotService.processMessage(anyString(), anyMap())).thenReturn(
                ChatbotResponseDto.builder()
                        .message("Claro, te comento")
                        .intentDetected("unknown")
                        .requiresHumanAgent(false)
                        .build()
        );

        Decision decision = strategy.decide(ctx);

        assertEquals(ActionType.SEND_TEXT, decision.getActionType());
        assertEquals("Bot responded to message", decision.getReason(),
                "Una cotización pendiente caducada no debe volver a pedir datos de facturación");
        assertFalse(testConversation.getMetadata().contains(pendingId),
                "La cotización pendiente caducada debe limpiarse de la metadata");
        assertFalse(testConversation.getMetadata().contains("pending_quote_created_at"),
                "La marca de tiempo de la cotización pendiente también debe limpiarse");
    }
}