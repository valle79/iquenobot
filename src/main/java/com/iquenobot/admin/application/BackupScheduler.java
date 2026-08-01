package com.iquenobot.admin.application;

import com.iquenobot.shared.application.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ejecuta respaldos automáticos según la configuración global del panel de Super Admin.
 * Comprueba cada 30 minutos si toca crear un respaldo según la frecuencia configurada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackupScheduler {

    private final DatabaseBackupService backupService;
    private final SystemSettingsService settingsService;

    @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void runScheduledBackup() {
        if (!settingsService.getBoolean("backups", "auto_backup", false)) {
            return;
        }

        int frequencyHours = settingsService.getInt("backups", "backup_frequency", 24);
        if (frequencyHours <= 0) {
            log.debug("Backup frequency invalid ({}), skipping scheduled backup", frequencyHours);
            return;
        }

        if (lastBackupBefore(frequencyHours)) {
            log.info("Running scheduled backup (frequency: {}h)", frequencyHours);
            try {
                backupService.createBackup();
            } catch (Exception e) {
                log.error("Scheduled backup failed: {}", e.getMessage(), e);
            }
        }
    }

    private boolean lastBackupBefore(int frequencyHours) {
        List<com.iquenobot.admin.domain.dto.BackupInfoDto> backups = backupService.listBackups();
        if (backups.isEmpty()) {
            return true;
        }
        LocalDateTime last = backups.get(0).getCreatedAt();
        return last.isBefore(LocalDateTime.now().minusHours(frequencyHours));
    }
}
