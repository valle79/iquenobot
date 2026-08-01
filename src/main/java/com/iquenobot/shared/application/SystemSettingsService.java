package com.iquenobot.shared.application;

import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service de acceso a la configuración global del sistema (Super Admin).
 * Lee las settings del tenant de sistema (00000000-0000-0000-0000-000000000000)
 * y las expone de forma tipada con valores por defecto. Mantiene una caché en
 * memoria que se invalida cuando se actualizan las settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsService {

    public static final UUID SYSTEM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final SettingRepository settingRepository;

    private final Map<String, Map<String, Setting>> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public String getString(String category, String key, String defaultValue) {
        Setting setting = findSetting(category, key);
        if (setting == null || setting.getValue() == null || setting.getValue().isBlank()) {
            return defaultValue;
        }
        return setting.getValue();
    }

    @Transactional(readOnly = true)
    public int getInt(String category, String key, int defaultValue) {
        String value = getString(category, key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for setting {}.{}: '{}'", category, key, value);
            return defaultValue;
        }
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String category, String key, boolean defaultValue) {
        String value = getString(category, key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value.trim());
    }

    @Transactional(readOnly = true)
    public List<Setting> getByCategory(String category) {
        return settingRepository.findByTenantIdAndCategoryAndDeletedFalse(SYSTEM_TENANT_ID, category);
    }

    public void evict(String category) {
        cache.remove(category);
        log.debug("System settings cache evicted for category: {}", category);
    }

    private Setting findSetting(String category, String key) {
        Map<String, Setting> categoryCache = cache.get(category);
        if (categoryCache != null && categoryCache.containsKey(key)) {
            return categoryCache.get(key);
        }

        Setting setting = settingRepository
                .findByTenantIdAndCategoryAndKeyAndDeletedFalse(SYSTEM_TENANT_ID, category, key)
                .orElse(null);

        cache.computeIfAbsent(category, k -> new ConcurrentHashMap<>()).put(key, setting);
        return setting;
    }

    public Map<String, String> asMap(String category) {
        return getByCategory(category).stream()
                .collect(Collectors.toMap(Setting::getKey, s -> s.getValue() == null ? "" : s.getValue()));
    }
}
