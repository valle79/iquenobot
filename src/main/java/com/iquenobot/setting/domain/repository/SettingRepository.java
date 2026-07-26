package com.iquenobot.setting.domain.repository;

import com.iquenobot.setting.domain.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettingRepository extends JpaRepository<Setting, UUID> {

    List<Setting> findByTenantIdAndDeletedFalse(UUID tenantId);

    List<Setting> findByTenantIdAndCategoryAndDeletedFalse(UUID tenantId, String category);

    Optional<Setting> findByTenantIdAndCategoryAndKeyAndDeletedFalse(UUID tenantId, String category, String key);

    boolean existsByTenantIdAndCategoryAndKeyAndDeletedFalse(UUID tenantId, String category, String key);

    Optional<Setting> findByCategoryAndKeyAndValueAndDeletedFalse(String category, String key, String value);
}
