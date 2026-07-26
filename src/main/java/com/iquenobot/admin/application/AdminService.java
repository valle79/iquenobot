package com.iquenobot.admin.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.admin.domain.dto.SystemStatsDto;
import com.iquenobot.admin.domain.dto.UpdateTenantRequestDto;
import com.iquenobot.auth.domain.dto.CreateTenantRequestDto;
import com.iquenobot.auth.domain.dto.CreateUserRequestDto;
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
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.TenantStatus;
import com.iquenobot.shared.enums.UserStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final RoleRepository roleRepository;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final TenantProvisioningService provisioningService;
    private final DataSource dataSource;

    // ========== TENANT CRUD ==========

    @Transactional(readOnly = true)
    public PagedResponse<TenantDto> getTenants(String search, String status, Pageable pageable) {
        Page<Tenant> page;
        if (search != null && !search.isBlank()) {
            page = tenantRepository.findBySearchAndDeletedFalse(search, pageable);
        } else if (status != null && !status.isBlank()) {
            try {
                TenantStatus tenantStatus = TenantStatus.valueOf(status.toUpperCase());
                page = tenantRepository.findByStatusAndDeletedFalse(tenantStatus, pageable);
            } catch (IllegalArgumentException e) {
                page = tenantRepository.findByDeletedFalse(pageable);
            }
        } else {
            page = tenantRepository.findByDeletedFalse(pageable);
        }

        return PagedResponse.<TenantDto>builder()
                .content(page.getContent().stream().map(this::toFullTenantDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public TenantDto getTenantById(UUID id) {
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
        return toFullTenantDto(tenant);
    }

    @Transactional
    public TenantDto createTenant(CreateTenantRequestDto request) {
        log.info("Admin provisioning new tenant: {}", request.getCompanyName());
        return provisioningService.provisionTenant(request);
    }

    @Transactional
    public TenantDto updateTenantStatus(UUID id, TenantStatus status) {
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        if (tenant.getId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            throw new BusinessException("No se puede modificar el tenant del sistema");
        }

        tenant.setStatus(status);
        tenant = tenantRepository.save(tenant);
        log.info("Tenant {} status updated to: {}", tenant.getCompanyName(), status);
        return toFullTenantDto(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        if (tenant.getId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            throw new BusinessException("No se puede eliminar el tenant del sistema");
        }

        tenant.softDelete(null);
        tenantRepository.save(tenant);
        log.info("Tenant soft-deleted: {}", tenant.getCompanyName());
    }

    @Transactional
    public TenantDto updateTenant(UUID id, UpdateTenantRequestDto request) {
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));

        if (tenant.getId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            throw new BusinessException("No se puede modificar el tenant del sistema");
        }

        if (request.getCompanyName() != null) tenant.setCompanyName(request.getCompanyName());
        if (request.getBusinessName() != null) tenant.setBusinessName(request.getBusinessName());
        if (request.getRuc() != null) tenant.setRuc(request.getRuc());
        if (request.getContactEmail() != null) tenant.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) tenant.setContactPhone(request.getContactPhone());
        if (request.getWebsiteUrl() != null) tenant.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getLogoUrl() != null) tenant.setLogoUrl(request.getLogoUrl());
        if (request.getAddress() != null) tenant.setAddress(request.getAddress());
        if (request.getCity() != null) tenant.setCity(request.getCity());
        if (request.getCountry() != null) tenant.setCountry(request.getCountry());
        if (request.getTimezone() != null) tenant.setTimezone(request.getTimezone());
        if (request.getCurrency() != null) tenant.setCurrency(request.getCurrency());
        if (request.getLanguage() != null) tenant.setLanguage(request.getLanguage());
        if (request.getLocale() != null) tenant.setLocale(request.getLocale());
        if (request.getPrimaryColor() != null) tenant.setPrimaryColor(request.getPrimaryColor());
        if (request.getSecondaryColor() != null) tenant.setSecondaryColor(request.getSecondaryColor());
        if (request.getSubscriptionPlan() != null) tenant.setSubscriptionPlan(request.getSubscriptionPlan());
        if (request.getMaxUsers() != null) tenant.setMaxUsers(request.getMaxUsers());
        if (request.getMaxConversations() != null) tenant.setMaxConversations(request.getMaxConversations());
        if (request.getPlanId() != null) {
            Plan plan = planRepository.findByIdAndDeletedFalse(request.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
            tenant.setPlan(plan);
        }

        if (request.getSubscriptionExpiresAt() != null) {
            tenant.setSubscriptionExpiresAt(request.getSubscriptionExpiresAt());
        }

        tenant = tenantRepository.save(tenant);
        log.info("Tenant updated: {}", tenant.getCompanyName());
        return toFullTenantDto(tenant);
    }

    // ========== PLANS ==========

    @Transactional(readOnly = true)
    public PagedResponse<com.iquenobot.plan.domain.dto.PlanDto> getPlans(Pageable pageable) {
        Page<Plan> page = planRepository.findByDeletedFalse(pageable);
        return PagedResponse.<com.iquenobot.plan.domain.dto.PlanDto>builder()
                .content(page.getContent().stream().map(this::toPlanDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public com.iquenobot.plan.domain.dto.PlanDto getPlanById(UUID id) {
        Plan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        return toPlanDto(plan);
    }

    @Transactional
    public com.iquenobot.plan.domain.dto.PlanDto createPlan(com.iquenobot.plan.domain.dto.CreatePlanRequestDto request) {
        if (planRepository.existsByCodeAndDeletedFalse(request.getCode())) {
            throw new BusinessException("El código del plan ya existe");
        }

        Plan plan = Plan.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .monthlyPrice(request.getMonthlyPrice())
                .yearlyPrice(request.getYearlyPrice())
                .maxUsers(request.getMaxUsers())
                .maxConversations(request.getMaxConversations())
                .maxContacts(request.getMaxContacts())
                .maxStorageMb(request.getMaxStorageMb())
                .features(request.getFeatures())
                .active(request.isActive())
                .publicPlan(request.isPublicPlan())
                .sortOrder(request.getSortOrder())
                .build();

        plan = planRepository.save(plan);
        log.info("Plan created: {}", plan.getName());
        return toPlanDto(plan);
    }

    @Transactional
    public com.iquenobot.plan.domain.dto.PlanDto updatePlan(UUID id, com.iquenobot.plan.domain.dto.UpdatePlanRequestDto request) {
        Plan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));

        if (request.getName() != null) plan.setName(request.getName());
        if (request.getCode() != null) plan.setCode(request.getCode());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getMonthlyPrice() != null) plan.setMonthlyPrice(request.getMonthlyPrice());
        if (request.getYearlyPrice() != null) plan.setYearlyPrice(request.getYearlyPrice());
        if (request.getMaxUsers() != null) plan.setMaxUsers(request.getMaxUsers());
        if (request.getMaxConversations() != null) plan.setMaxConversations(request.getMaxConversations());
        if (request.getMaxContacts() != null) plan.setMaxContacts(request.getMaxContacts());
        if (request.getMaxStorageMb() != null) plan.setMaxStorageMb(request.getMaxStorageMb());
        if (request.getFeatures() != null) plan.setFeatures(request.getFeatures());
        if (request.getActive() != null) plan.setActive(request.getActive());
        if (request.getPublicPlan() != null) plan.setPublicPlan(request.getPublicPlan());
        if (request.getSortOrder() != null) plan.setSortOrder(request.getSortOrder());

        plan = planRepository.save(plan);
        log.info("Plan updated: {}", plan.getName());
        return toPlanDto(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        Plan plan = planRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
        plan.softDelete(null);
        planRepository.save(plan);
        log.info("Plan soft-deleted: {}", plan.getName());
    }

    // ========== SYSTEM STATS ==========

    @Transactional(readOnly = true)
    public SystemStatsDto getSystemStats() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();

        String dbStatus = "healthy";
        int activeConnections = 0;
        int maxConnections = 0;
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            maxConnections = meta.getMaxConnections();
            activeConnections = 5;
        } catch (Exception e) {
            dbStatus = "error: " + e.getMessage();
        }

        return SystemStatsDto.builder()
                .totalTenants(tenantRepository.countByDeletedFalse())
                .activeTenants(tenantRepository.countActiveTenants())
                .suspendedTenants(tenantRepository.countByStatusAndDeletedFalse(TenantStatus.SUSPENDED))
                .trialTenants(tenantRepository.countTrialTenants())
                .expiredTenants(tenantRepository.countExpiredTenants())
                .totalUsers(userRepository.countAllActive())
                .totalConversations(0)
                .totalMessages(0)
                .totalContacts(0)
                .totalLeads(0)
                .totalProducts(0)
                .storageUsedMb(0)
                .aiTotalRequests(0)
                .aiTotalTokens(0)
                .server(SystemStatsDto.ServerStatusDto.builder()
                        .status("healthy")
                        .version(runtime.getSpecVersion())
                        .uptime(formatDuration(runtime.getUptime()))
                        .cpuUsage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage())
                        .memoryUsage(heap.getUsed() / 1024.0 / 1024.0)
                        .memoryMax(heap.getMax() / 1024.0 / 1024.0)
                        .activeThreads(threads.getThreadCount())
                        .build())
                .database(SystemStatsDto.DatabaseStatusDto.builder()
                        .status(dbStatus)
                        .activeConnections(activeConnections)
                        .maxConnections(maxConnections)
                        .diskUsageMb(0)
                        .build())
                .evolutionApi(SystemStatsDto.IntegrationStatusDto.builder()
                        .status("disconnected")
                        .lastCheck(LocalDateTime.now().toString())
                        .build())
                .build();
    }

    // ========== MAPPERS ==========

    private TenantDto toFullTenantDto(Tenant tenant) {
        TenantDto dto = authMapper.toTenantDto(tenant);
        if (tenant.getPlan() != null) {
            dto.setPlan(toPlanDto(tenant.getPlan()));
        }
        return dto;
    }

    private com.iquenobot.plan.domain.dto.PlanDto toPlanDto(Plan plan) {
        return com.iquenobot.plan.domain.dto.PlanDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .code(plan.getCode())
                .description(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice())
                .yearlyPrice(plan.getYearlyPrice())
                .maxUsers(plan.getMaxUsers())
                .maxConversations(plan.getMaxConversations())
                .maxContacts(plan.getMaxContacts())
                .maxStorageMb(plan.getMaxStorageMb())
                .features(plan.getFeatures())
                .active(plan.isActive())
                .publicPlan(plan.isPublicPlan())
                .sortOrder(plan.getSortOrder())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private String formatDuration(long millis) {
        Duration d = Duration.ofMillis(millis);
        long days = d.toDays();
        long hours = d.toHours() % 24;
        long minutes = d.toMinutes() % 60;
        return String.format("%dd %dh %dm", days, hours, minutes);
    }
}
