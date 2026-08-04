package com.iquenobot.admin.application;

import com.iquenobot.auth.domain.dto.CreateTenantRequestDto;
import com.iquenobot.auth.domain.dto.TenantDto;
import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.auth.interfaces.mapper.AuthMapper;
import com.iquenobot.plan.domain.entity.Plan;
import com.iquenobot.plan.domain.repository.PlanRepository;
import com.iquenobot.role.domain.entity.Role;
import com.iquenobot.role.domain.repository.RoleRepository;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.TenantStatus;
import com.iquenobot.shared.enums.UserStatus;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final RoleRepository roleRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final DefaultDataSeeder dataSeeder;

    @Transactional
    public TenantDto provisionTenant(CreateTenantRequestDto request) {
        log.info("Starting tenant provisioning for: {}", request.getCompanyName());

        // 1. Validate uniqueness
        if (tenantRepository.existsByCompanyNameAndDeletedFalse(request.getCompanyName().trim())) {
            throw new BusinessException("El nombre de la empresa ya está en uso");
        }
        if (tenantRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new BusinessException("El subdominio ya está en uso");
        }
        if (request.getWebsiteUrl() != null && !request.getWebsiteUrl().isBlank()
                && tenantRepository.existsByWebsiteUrlAndDeletedFalse(request.getWebsiteUrl().trim())) {
            throw new BusinessException("El sitio web ya está registrado por otra empresa");
        }
        if (tenantRepository.existsByContactEmailAndDeletedFalse(request.getContactEmail())) {
            throw new BusinessException("El email de contacto ya está registrado");
        }

        // 2. Resolve plan
        Plan plan = null;
        if (request.getSubscriptionPlan() != null) {
            plan = planRepository.findByCodeAndDeletedFalse(request.getSubscriptionPlan()).orElse(null);
        }
        if (plan == null) {
            plan = planRepository.findByCodeAndDeletedFalse("basic").orElse(null);
        }

        // 3. Create Tenant
        Tenant tenant = authMapper.toTenant(request);
        tenant.setId(UUID.randomUUID());
        tenant.setTenantId(tenant.getId());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSubscriptionExpiresAt(LocalDate.now().plusDays(30));
        tenant.setPlan(plan);

        if (plan != null) {
            tenant.setSubscriptionPlan(plan.getCode());
            tenant.setMaxUsers(plan.getMaxUsers());
            tenant.setMaxConversations(plan.getMaxConversations());
        }

        // Apply regional defaults
        if (request.getTimezone() == null) tenant.setTimezone("UTC");
        if (request.getCurrency() == null) tenant.setCurrency("PEN");
        if (request.getLanguage() == null) tenant.setLanguage("es");
        if (request.getLocale() == null) tenant.setLocale("es-PE");
        if (request.getPrimaryColor() == null) tenant.setPrimaryColor("#6366f1");
        if (request.getSecondaryColor() == null) tenant.setSecondaryColor("#818cf8");

        tenant = tenantRepository.save(tenant);
        log.info("Tenant created: {} (id={})", tenant.getCompanyName(), tenant.getId());

        // 4. Create TENANT_ADMIN user
        User admin = createTenantAdmin(request, tenant);
        log.info("Tenant admin created: {} (id={})", admin.getEmail(), admin.getId());

        // 5. Seed default roles
        dataSeeder.seedDefaultRoles(tenant.getId());
        log.info("Default roles seeded for tenant: {}", tenant.getId());

        // 6. Seed default settings
        dataSeeder.seedDefaultSettings(tenant.getId());
        log.info("Default settings seeded for tenant: {}", tenant.getId());

        // 7. Seed default categories
        dataSeeder.seedDefaultCategories(tenant.getId());
        log.info("Default categories seeded for tenant: {}", tenant.getId());

        // 8. Seed default pipeline + stages
        dataSeeder.seedDefaultPipeline(tenant.getId());
        log.info("Default pipeline seeded for tenant: {}", tenant.getId());

        // 9. Seed default tags
        dataSeeder.seedDefaultTags(tenant.getId());
        log.info("Default tags seeded for tenant: {}", tenant.getId());

        // 10. Seed AI configuration
        dataSeeder.seedDefaultAiConfig(tenant.getId());
        log.info("Default AI config seeded for tenant: {}", tenant.getId());

        // 11. Seed WhatsApp configuration
        dataSeeder.seedDefaultWhatsAppConfig(tenant.getId());
        log.info("Default WhatsApp config seeded for tenant: {}", tenant.getId());

        // 12. Seed notification preferences
        dataSeeder.seedDefaultNotificationPreferences(tenant.getId());
        log.info("Default notification prefs seeded for tenant: {}", tenant.getId());

        // 13. Seed security defaults
        dataSeeder.seedDefaultSecurityConfig(tenant.getId());
        log.info("Default security config seeded for tenant: {}", tenant.getId());

        // 14. Seed default chatbot intents
        dataSeeder.seedDefaultChatbotIntents(tenant.getId());
        log.info("Default chatbot intents seeded for tenant: {}", tenant.getId());

        // 15. Seed default chatbot flows
        dataSeeder.seedDefaultChatbotFlows(tenant.getId());
        log.info("Default chatbot flows seeded for tenant: {}", tenant.getId());

        log.info("Tenant provisioning COMPLETE for: {} (id={})", tenant.getCompanyName(), tenant.getId());
        return toTenantDto(tenant);
    }

    private User createTenantAdmin(CreateTenantRequestDto request, Tenant tenant) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant.getId())
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .phone(request.getAdminPhone())
                .role(RoleType.TENANT_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .tenant(tenant)
                .mustChangePassword(true)
                .build();

        return userRepository.save(user);
    }

    private TenantDto toTenantDto(Tenant tenant) {
        TenantDto dto = authMapper.toTenantDto(tenant);
        if (tenant.getPlan() != null) {
            dto.setPlan(com.iquenobot.plan.domain.dto.PlanDto.builder()
                    .id(tenant.getPlan().getId())
                    .name(tenant.getPlan().getName())
                    .code(tenant.getPlan().getCode())
                    .monthlyPrice(tenant.getPlan().getMonthlyPrice())
                    .yearlyPrice(tenant.getPlan().getYearlyPrice())
                    .maxUsers(tenant.getPlan().getMaxUsers())
                    .maxConversations(tenant.getPlan().getMaxConversations())
                    .features(tenant.getPlan().getFeatures())
                    .build());
        }
        return dto;
    }
}
