package com.iquenobot.conversation.application;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.enums.ConversationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationHandoffServiceTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CONVERSATION_ID = UUID.randomUUID();
    private static final UUID AGENT_ID = UUID.randomUUID();

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private SettingRepository settingRepository;

    private ConversationHandoffService handoffService;

    @BeforeEach
    void setUp() {
        handoffService = new ConversationHandoffService(conversationRepository, settingRepository);
        lenient().when(settingRepository.findByTenantIdAndDeletedFalse(TENANT_ID)).thenReturn(List.of());
    }

    private Conversation conversation() {
        return Conversation.builder()
                .id(CONVERSATION_ID)
                .tenantId(TENANT_ID)
                .status(ConversationStatus.OPEN)
                .build();
    }

    @Test
    void shouldAllowBotWhenNoHumanHandoff() {
        Conversation conversation = conversation();

        assertTrue(handoffService.canBotRespond(conversation));
        assertFalse(conversation.isHumanHandoff());
    }

    @Test
    void shouldPauseBotWhenAgentRepliedRecently() {
        Conversation conversation = conversation();
        conversation.activateHumanHandoff(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(3));

        assertFalse(handoffService.canBotRespond(conversation));
        assertTrue(conversation.isHumanHandoff());
    }

    @Test
    void shouldResumeBotAfterTimeout() {
        Conversation conversation = conversation();
        conversation.activateHumanHandoff(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

        assertTrue(handoffService.canBotRespond(conversation));
        assertFalse(conversation.isHumanHandoff());
        assertNull(conversation.getBotResumeAfter());
    }

    @Test
    void shouldForceResumeAfterMaxHumanIdle() {
        Conversation conversation = conversation();
        conversation.setHumanHandoff(true);
        conversation.setHumanTakenOverAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(20));
        conversation.setBotResumeAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2));

        assertTrue(handoffService.canBotRespond(conversation));
        assertFalse(conversation.isHumanHandoff());
    }

    @Test
    void shouldNeverRespondWhenClosed() {
        Conversation conversation = conversation();
        conversation.setStatus(ConversationStatus.CLOSED);
        conversation.setHumanHandoff(true);

        assertFalse(handoffService.canBotRespond(conversation));
    }

    @Test
    void shouldNotResumeWhenAutoResumeDisabled() {
        Conversation conversation = conversation();
        conversation.activateHumanHandoff(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10));

        when(settingRepository.findByTenantIdAndDeletedFalse(TENANT_ID)).thenReturn(List.of(
                setting("bot", "bot_resume_enabled", "false")));

        assertFalse(handoffService.canBotRespond(conversation));
        assertTrue(conversation.isHumanHandoff());
    }

    @Test
    void shouldPersistHandoffOnAgentMessage() {
        Conversation conversation = conversation();

        LocalDateTime resumeAfter = handoffService.onAgentMessage(conversation, AGENT_ID);

        assertNotNull(resumeAfter);
        assertTrue(resumeAfter.isAfter(LocalDateTime.now(ZoneOffset.UTC)));
        verify(conversationRepository).activateHumanHandoff(
                eq(CONVERSATION_ID), eq(TENANT_ID), any(), any(), any());
    }

    @Test
    void shouldRefreshWindowOnConsecutiveAgentMessages() {
        Conversation conversation = conversation();

        handoffService.onAgentMessage(conversation, AGENT_ID);
        handoffService.onAgentMessage(conversation, AGENT_ID);

        verify(conversationRepository, org.mockito.Mockito.times(2)).activateHumanHandoff(
                eq(CONVERSATION_ID), eq(TENANT_ID), any(), any(), any());
    }

    private Setting setting(String category, String key, String value) {
        return Setting.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .category(category)
                .key(key)
                .value(value)
                .build();
    }
}
