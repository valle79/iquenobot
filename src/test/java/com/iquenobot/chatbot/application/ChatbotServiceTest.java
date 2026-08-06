package com.iquenobot.chatbot.application;

import com.iquenobot.ai.domain.service.IAIProvider;
import com.iquenobot.chatbot.domain.dto.ChatbotResponseDto;
import com.iquenobot.chatbot.domain.repository.ChatbotFlowRepository;
import com.iquenobot.chatbot.domain.repository.ChatbotIntentRepository;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.knowledge.application.KnowledgeBaseService;
import com.iquenobot.shared.domain.util.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ChatbotFlowRepository flowRepository;

    @Mock
    private ChatbotIntentRepository intentRepository;

    @Mock
    private IAIProvider aiProvider;

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ChatbotService chatbotService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private void stubNoIntentOrFlow() {
        when(intentRepository.findByTenantIdAndActiveAndDeletedFalseOrderByPriorityDesc(eq(tenantId), eq(true)))
                .thenReturn(List.of());
        when(flowRepository.findByKeyword(eq(tenantId), anyString())).thenReturn(List.of());
    }

    @Test
    void processMessage_shouldGreetCordially_whenSimpleGreeting() {
        stubNoIntentOrFlow();
        Map<String, Object> context = Map.of("contactId", UUID.randomUUID().toString());

        ChatbotResponseDto response = chatbotService.processMessage("Hola", context, true);

        assertFalse(response.isRequiresClarification());
        assertFalse(response.isRequiresHumanAgent());
        assertTrue(response.getMessage().startsWith("¡Hola! Mucho gusto saludarte."));
        assertFalse(response.getMessage().toLowerCase().contains("no logré entender"),
                "Un saludo jamás debe responder con el fallback de aclaración");
    }

    @Test
    void processMessage_shouldPersonalizeGreeting_whenNameKnown() {
        stubNoIntentOrFlow();
        Contact contact = new Contact();
        contact.setFirstName("María");
        when(contactRepository.findByIdAndTenantIdAndDeletedFalse(any(), eq(tenantId)))
                .thenReturn(Optional.of(contact));
        Map<String, Object> context = Map.of("contactId", UUID.randomUUID().toString());

        ChatbotResponseDto response = chatbotService.processMessage("Buenos días", context, true);

        assertTrue(response.getMessage().contains("¡Hola, María!"));
    }

    @Test
    void processMessage_shouldNotTreatQuoteRequestAsGreeting() {
        stubNoIntentOrFlow();
        when(knowledgeBaseService.buildContextForQuery(anyString())).thenReturn("");
        when(aiProvider.chatCompletion(anyList(), any(), any(), any())).thenReturn(
                com.iquenobot.ai.domain.dto.AIResponseDto.builder()
                        .content("Claro, te ayudo con tu cotización.")
                        .build());
        Map<String, Object> context = Map.of("contactId", UUID.randomUUID().toString());

        ChatbotResponseDto response = chatbotService.processMessage("quiero cotizar la cosechadora", context, true);

        assertFalse(response.getMessage().startsWith("¡Hola!"),
                "Una solicitud de cotización no es un saludo");
        assertFalse(response.isRequiresClarification());
        assertEquals("solicitar_precio", response.getIntentDetected());
    }
}
