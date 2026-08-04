package com.iquenobot.shared.config;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.plan.domain.entity.Plan;
import com.iquenobot.plan.domain.repository.PlanRepository;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.TenantStatus;
import com.iquenobot.shared.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final UUID SYSTEM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String SUPER_ADMIN_EMAIL = "superadmin@iquenobot.com";
    private static final String ALL_FEATURES = "{\"whatsapp\":true,\"ai_assistant\":true,\"reports\":true,\"api_access\":true,\"custom_branding\":true,\"multi_agent\":true}";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    private record PlanSeed(
            String name,
            String code,
            String description,
            String monthlyPrice,
            String yearlyPrice,
            Integer maxUsers,
            Integer maxConversations,
            Integer maxContacts,
            Integer maxStorageMb,
            String features,
            int sortOrder) {

        Plan toEntity() {
            return Plan.builder()
                    .id(UUID.randomUUID())
                    .tenantId(SYSTEM_TENANT_ID)
                    .name(name)
                    .code(code)
                    .description(description)
                    .monthlyPrice(new BigDecimal(monthlyPrice))
                    .yearlyPrice(new BigDecimal(yearlyPrice))
                    .maxUsers(maxUsers)
                    .maxConversations(maxConversations)
                    .maxContacts(maxContacts)
                    .maxStorageMb(maxStorageMb)
                    .features(features)
                    .active(true)
                    .publicPlan(true)
                    .sortOrder(sortOrder)
                    .build();
        }
    }

    private static final List<PlanSeed> PLAN_SEEDS = List.of(
            new PlanSeed("Básico", "basic", "Plan básico para pequeñas empresas",
                    "29.99", "299.99", 15, 500, 2000, null, ALL_FEATURES, 1),
            new PlanSeed("Profesional", "professional", "Plan profesional para empresas en crecimiento",
                    "79.99", "799.99", 50, 2500, 10000, null, ALL_FEATURES, 2),
            new PlanSeed("Enterprise", "enterprise", "Plan enterprise para grandes organizaciones",
                    "199.99", "1999.99", null, null, null, null, ALL_FEATURES, 3));

    @Override
    public void run(String... args) {
        seedPlans();
        seedSystemTenantAndSuperAdmin();
    }

    private void seedPlans() {
        log.info("Syncing default plans...");

        PLAN_SEEDS.forEach(seed -> {
            planRepository.findByCode(seed.code())
                    .ifPresentOrElse(existing -> {
                        if (existing.isDeleted()) {
                            log.info("Plan eliminado por el administrador, no se restaura: {}", seed.code());
                            return;
                        }
                        existing.setName(seed.name());
                        existing.setDescription(seed.description());
                        existing.setMonthlyPrice(new BigDecimal(seed.monthlyPrice()));
                        existing.setYearlyPrice(new BigDecimal(seed.yearlyPrice()));
                        existing.setMaxUsers(seed.maxUsers());
                        existing.setMaxConversations(seed.maxConversations());
                        existing.setMaxContacts(seed.maxContacts());
                        existing.setMaxStorageMb(seed.maxStorageMb());
                        existing.setFeatures(seed.features());
                        existing.setSortOrder(seed.sortOrder());
                        existing.setActive(true);
                        existing.setPublicPlan(true);

                        planRepository.save(existing);

                        log.info("Plan sincronizado: {}", seed.code());
                    }, () -> {
                        planRepository.save(seed.toEntity());

                        log.info("Plan creado: {}", seed.code());
                    });
        });

        deactivateLegacyFreePlan();
    }

    private void deactivateLegacyFreePlan() {
        planRepository.findByCode("free").ifPresent(free -> {
            if (free.isActive()) {
                free.setActive(false);
                free.setPublicPlan(false);
                planRepository.save(free);
                log.info("Free plan deactivated (no longer offered)");
            }
        });
    }

    private void seedSystemTenantAndSuperAdmin() {
        if (userRepository.findAll().stream().anyMatch(u -> u.getRole() == RoleType.SUPER_ADMIN && !u.isDeleted())) {
            log.info("SUPER_ADMIN already exists, skipping initialization");
            return;
        }

        log.info("Creating system tenant and SUPER_ADMIN user...");

        Plan enterprisePlan = planRepository.findByCode("enterprise").orElse(null);

        Tenant systemTenant = Tenant.builder()
                .id(SYSTEM_TENANT_ID)
                .tenantId(SYSTEM_TENANT_ID)
                .companyName("IquenoBot System")
                .subdomain("system")
                .contactEmail("system@iquenobot.com")
                .status(TenantStatus.ACTIVE)
                .subscriptionExpiresAt(LocalDate.now().plusYears(100))
                .maxUsers(1000)
                .maxConversations(100000)
                .plan(enterprisePlan)
                .build();

        tenantRepository.save(systemTenant);

        User superAdmin = User.builder()
                .id(UUID.randomUUID())
                .tenantId(SYSTEM_TENANT_ID)
                .email(SUPER_ADMIN_EMAIL)
                .password(passwordEncoder.encode("IquenoBot@SuperAdmin2026!"))
                .firstName("Super")
                .lastName("Admin")
                .role(RoleType.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        userRepository.save(superAdmin);

        log.info("SUPER_ADMIN created: {} in system tenant", SUPER_ADMIN_EMAIL);
    }
}
