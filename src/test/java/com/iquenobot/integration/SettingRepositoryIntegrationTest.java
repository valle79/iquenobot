package com.iquenobot.integration;

import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SettingRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private SettingRepository settingRepository;

    @Test
    void shouldSaveAndFindSettingByCategoryAndKeyAndValue() {
        UUID tenantId = UUID.randomUUID();

        Setting setting = Setting.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .category("whatsapp")
                .key("instance_id")
                .value("test-instance-123")
                .type("text")
                .description("WhatsApp instance ID")
                .build();

        settingRepository.save(setting);

        Optional<Setting> found = settingRepository
                .findByCategoryAndKeyAndValueAndDeletedFalse("whatsapp", "instance_id", "test-instance-123");

        assertTrue(found.isPresent());
        assertEquals(tenantId, found.get().getTenantId());
        assertEquals("test-instance-123", found.get().getValue());
    }

    @Test
    void shouldNotFindDeletedSetting() {
        UUID tenantId = UUID.randomUUID();

        Setting setting = Setting.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .category("whatsapp")
                .key("instance_id")
                .value("deleted-instance")
                .type("text")
                .deleted(true)
                .build();

        settingRepository.save(setting);

        Optional<Setting> found = settingRepository
                .findByCategoryAndKeyAndValueAndDeletedFalse("whatsapp", "instance_id", "deleted-instance");

        assertFalse(found.isPresent());
    }

    @Test
    void shouldFindSettingsByTenantAndCategory() {
        UUID tenantId = UUID.randomUUID();

        Setting s1 = Setting.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .category("general").key("language").value("es").build();
        Setting s2 = Setting.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .category("general").key("timezone").value("UTC").build();

        settingRepository.save(s1);
        settingRepository.save(s2);

        var settings = settingRepository.findByTenantIdAndCategoryAndDeletedFalse(tenantId, "general");

        assertEquals(2, settings.size());
    }
}
