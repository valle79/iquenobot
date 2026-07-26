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
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final UUID SYSTEM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String SUPER_ADMIN_EMAIL = "superadmin@iquenobot.com";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedPlans();
        seedSystemTenantAndSuperAdmin();
    }

    private void seedPlans() {
        if (planRepository.count() > 0) {
            log.info("Plans already seeded, skipping");
            return;
        }

        log.info("Seeding default plans...");

        UUID systemId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        planRepository.save(Plan.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .tenantId(systemId)
                .name("Free")
                .code("free")
                .description("Plan gratuito para probar la plataforma")
                .monthlyPrice(BigDecimal.ZERO)
                .yearlyPrice(BigDecimal.ZERO)
                .maxUsers(1)
                .maxConversations(50)
                .maxContacts(100)
                .maxStorageMb(50)
                .features("{\"whatsapp\":false,\"ai_assistant\":false,\"reports\":false,\"api_access\":false,\"custom_branding\":false,\"multi_agent\":false}")
                .active(true)
                .publicPlan(true)
                .sortOrder(1)
                .build());

        planRepository.save(Plan.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .tenantId(systemId)
                .name("Basic")
                .code("basic")
                .description("Plan básico para pequeñas empresas")
                .monthlyPrice(new BigDecimal("29.99"))
                .yearlyPrice(new BigDecimal("299.99"))
                .maxUsers(3)
                .maxConversations(500)
                .maxContacts(1000)
                .maxStorageMb(500)
                .features("{\"whatsapp\":true,\"ai_assistant\":false,\"reports\":true,\"api_access\":false,\"custom_branding\":false,\"multi_agent\":false}")
                .active(true)
                .publicPlan(true)
                .sortOrder(2)
                .build());

        planRepository.save(Plan.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000003"))
                .tenantId(systemId)
                .name("Professional")
                .code("professional")
                .description("Plan profesional para empresas en crecimiento")
                .monthlyPrice(new BigDecimal("79.99"))
                .yearlyPrice(new BigDecimal("799.99"))
                .maxUsers(10)
                .maxConversations(5000)
                .maxContacts(10000)
                .maxStorageMb(2000)
                .features("{\"whatsapp\":true,\"ai_assistant\":true,\"reports\":true,\"api_access\":true,\"custom_branding\":false,\"multi_agent\":false}")
                .active(true)
                .publicPlan(true)
                .sortOrder(3)
                .build());

        planRepository.save(Plan.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000004"))
                .tenantId(systemId)
                .name("Enterprise")
                .code("enterprise")
                .description("Plan enterprise para grandes organizaciones")
                .monthlyPrice(new BigDecimal("199.99"))
                .yearlyPrice(new BigDecimal("1999.99"))
                .maxUsers(999999)
                .maxConversations(999999)
                .maxContacts(999999)
                .maxStorageMb(10000)
                .features("{\"whatsapp\":true,\"ai_assistant\":true,\"reports\":true,\"api_access\":true,\"custom_branding\":true,\"multi_agent\":true}")
                .active(true)
                .publicPlan(true)
                .sortOrder(4)
                .build());

        log.info("Default plans seeded successfully");
    }

    private void seedSystemTenantAndSuperAdmin() {
        if (userRepository.findAll().stream().anyMatch(u -> u.getRole() == RoleType.SUPER_ADMIN && !u.isDeleted())) {
            log.info("SUPER_ADMIN already exists, skipping initialization");
            return;
        }

        log.info("Creating system tenant and SUPER_ADMIN user...");

        Plan enterprisePlan = planRepository.findByCodeAndDeletedFalse("enterprise").orElse(null);

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
