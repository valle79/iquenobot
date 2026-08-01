package com.iquenobot.auth.application;

import com.iquenobot.auth.domain.repository.UserSessionRepository;
import com.iquenobot.shared.application.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cierra automáticamente las sesiones de usuario inactivas según el
 * timeout configurado globalmente en el panel de Super Admin.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionTimeoutScheduler {

    private final UserSessionRepository userSessionRepository;
    private final SystemSettingsService settingsService;

    @Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void expireIdleSessions() {
        int timeoutMinutes = settingsService.getInt("security", "session_timeout", 480);
        if (timeoutMinutes <= 0) {
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        int expired = userSessionRepository.endExpiredSessions(cutoff, LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} idle user sessions (timeout: {} min)", expired, timeoutMinutes);
        }
    }
}
