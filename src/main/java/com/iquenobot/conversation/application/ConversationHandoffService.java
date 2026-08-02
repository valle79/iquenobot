package com.iquenobot.conversation.application;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.orchestrator.domain.model.BotConfiguration;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.enums.ConversationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Controla la pausa temporal del bot cuando un agente humano interviene
 * realmente en la conversación (handoff humano), y su recuperación
 * automática cuando el agente queda inactivo.
 *
 * Regla: el bot NO se detiene por estar la conversación asignada; se detiene
 * solo cuando un humano ha respondido. Tras la ventana configurada sin
 * actividad del agente, el bot reanuda la atención automáticamente.
 *
 * Configuración multitenant (settings, categoría "bot"):
 * - bot.bot_resume_enabled        (default true)  : activa la recuperación automática
 * - bot.bot_resume_delay_minutes  (default 3)     : ventana de pausa tras cada mensaje del agente
 * - bot.max_human_idle_minutes    (default 15)    : techo de inactividad total que fuerza la reactivación
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationHandoffService {

    private static final int PAUSED_LOG_INTERVAL_SECONDS = 60;

    private final ConversationRepository conversationRepository;
    private final SettingRepository settingRepository;

    private final Map<UUID, LocalDateTime> lastPausedLogAt = new ConcurrentHashMap<>();

    /**
     * Pausa el bot: se invoca cuando un agente humano envía un mensaje desde
     * el panel del CRM. Persiste con update atómico (evita optimistic locks).
     *
     * @return instante en el que el bot podrá reanudarse (null si la
     *         recuperación automática está deshabilitada).
     */
    public LocalDateTime onAgentMessage(Conversation conversation, UUID agentId) {
        BotConfiguration config = loadBotConfig(conversation.getTenantId());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime resumeAfter = config.isBotResumeEnabled()
                ? now.plusMinutes(Math.max(1, config.getBotResumeDelayMinutes()))
                : null;

        conversationRepository.activateHumanHandoff(
                conversation.getId(),
                conversation.getTenantId(),
                now,
                now,
                resumeAfter);

        if (resumeAfter != null) {
            log.info("Human handoff activated: conversation={} agent={}; bot paused until {}",
                    conversation.getId(), agentId, resumeAfter);
        } else {
            log.info("Human handoff activated: conversation={} agent={}; bot resume disabled",
                    conversation.getId(), agentId);
        }
        return resumeAfter;
    }

    /**
     * Determina si la IA puede responder ahora mismo a esta conversación.
     *
     * - CLOSED/RESOLVED -> nunca responde.
     * - Sin handoff humano -> responde.
     * - Con handoff dentro de la ventana de pausa -> NO responde.
     * - Ventana vencida o techo de inactividad superado -> reactiva el bot
     *   (muta la entidad en memoria; el llamador debe persistirla) y responde.
     */
    public boolean canBotRespond(Conversation conversation) {
        if (conversation.getStatus() == ConversationStatus.CLOSED
                || conversation.getStatus() == ConversationStatus.RESOLVED) {
            return false;
        }

        if (!conversation.isHumanHandoff()) {
            return true;
        }

        BotConfiguration config = loadBotConfig(conversation.getTenantId());
        if (!config.isBotResumeEnabled()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        boolean windowExpired = conversation.getBotResumeAfter() == null
                || !now.isBefore(conversation.getBotResumeAfter());

        boolean idleLimitExceeded = conversation.getHumanTakenOverAt() != null
                && config.getMaxHumanIdleMinutes() > 0
                && now.isAfter(conversation.getHumanTakenOverAt()
                        .plusMinutes(config.getMaxHumanIdleMinutes()));

        if (!windowExpired && !idleLimitExceeded) {
            logPaused(conversation);
            return false;
        }

        conversation.resumeBot();
        log.info("Bot resumed conversation after agent inactivity: {}", conversation.getId());
        return true;
    }

    /**
     * Reactiva el bot si el agente está inactivo. Idempotente: si no hay
     * handoff o la ventana aún no venció, no hace nada.
     */
    public boolean resumeBotIfAgentIdle(Conversation conversation) {
        if (!conversation.isHumanHandoff()
                || conversation.getStatus() == ConversationStatus.CLOSED
                || conversation.getStatus() == ConversationStatus.RESOLVED) {
            return false;
        }
        return canBotRespond(conversation);
    }

    private void logPaused(Conversation conversation) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime last = lastPausedLogAt.get(conversation.getId());
        if (last == null || now.isAfter(last.plusSeconds(PAUSED_LOG_INTERVAL_SECONDS))) {
            lastPausedLogAt.put(conversation.getId(), now);
            log.info("Bot paused by human handoff: conversation={} until {}",
                    conversation.getId(), conversation.getBotResumeAfter());
        } else {
            log.debug("Bot paused by human handoff: conversation={} (retry)", conversation.getId());
        }
    }

    private BotConfiguration loadBotConfig(UUID tenantId) {
        if (tenantId == null) {
            return BotConfiguration.defaults();
        }
        Map<String, Setting> settingsByKey = settingRepository
                .findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getCategory() + "." + s.getKey(),
                        s -> s,
                        (a, b) -> a
                ));
        return BotConfiguration.fromSettings(settingsByKey);
    }
}
