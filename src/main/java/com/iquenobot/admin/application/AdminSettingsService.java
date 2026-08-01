package com.iquenobot.admin.application;

import com.iquenobot.setting.domain.dto.SettingDto;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.application.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSettingsService {

    private static final UUID SYSTEM_TENANT_ID = SystemSettingsService.SYSTEM_TENANT_ID;

    private final SettingRepository settingRepository;
    private final SystemSettingsService systemSettingsService;

    @Transactional(readOnly = true)
    public List<SettingDto> getByCategory(String category) {
        return settingRepository.findByTenantIdAndCategoryAndDeletedFalse(SYSTEM_TENANT_ID, category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<SettingDto> updateSettings(String category, Map<String, String> settings) {
        List<Setting> existing = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(SYSTEM_TENANT_ID, category);

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            Setting setting = existing.stream()
                    .filter(s -> s.getKey().equals(entry.getKey()))
                    .findFirst()
                    .orElse(Setting.builder()
                            .id(UUID.randomUUID())
                            .tenantId(SYSTEM_TENANT_ID)
                            .category(category)
                            .key(entry.getKey())
                            .build());

            setting.setValue(entry.getValue());
            settingRepository.save(setting);
        }

        systemSettingsService.evict(category);

        return settingRepository.findByTenantIdAndCategoryAndDeletedFalse(SYSTEM_TENANT_ID, category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private SettingDto toDto(Setting setting) {
        return SettingDto.builder()
                .id(setting.getId())
                .category(setting.getCategory())
                .key(setting.getKey())
                .value(setting.getValue())
                .type(setting.getType())
                .description(setting.getDescription())
                .build();
    }
}
