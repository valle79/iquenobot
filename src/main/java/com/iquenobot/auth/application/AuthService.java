package com.iquenobot.auth.application;

import com.iquenobot.auth.domain.dto.AuthResponseDto;
import com.iquenobot.auth.domain.dto.CreateTenantRequestDto;
import com.iquenobot.auth.domain.dto.CreateUserRequestDto;
import com.iquenobot.auth.domain.dto.LoginRequestDto;
import com.iquenobot.auth.domain.dto.TenantDto;
import com.iquenobot.auth.domain.dto.ChangePasswordRequestDto;
import com.iquenobot.auth.domain.dto.ForgotPasswordRequestDto;
import com.iquenobot.auth.domain.dto.ResetPasswordRequestDto;
import com.iquenobot.auth.domain.dto.UpdateMyProfileRequestDto;
import com.iquenobot.auth.domain.dto.UpdateUserRequestDto;
import com.iquenobot.auth.domain.dto.UserDto;
import com.iquenobot.auth.domain.entity.RefreshToken;
import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.entity.UserSession;
import com.iquenobot.auth.domain.repository.RefreshTokenRepository;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.auth.domain.repository.UserSessionRepository;

import com.iquenobot.auth.interfaces.mapper.AuthMapper;
import com.iquenobot.security.application.JwtService;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.TenantStatus;
import com.iquenobot.shared.enums.UserStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import com.iquenobot.shared.exception.UnauthorizedException;
import com.iquenobot.shared.application.EmailService;
import com.iquenobot.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public AuthResponseDto login(LoginRequestDto loginRequest) {
        log.info("Attempting login for email: {}", loginRequest.getEmail());

        // Find tenant by subdomain (would need to be passed separately)
        // For now, we'll find user by email and get tenant from user
        User user = findUserByEmail(loginRequest.getEmail());
        
        // Set tenant context
        TenantContext.setTenantId(user.getTenantId().toString());
        TenantContext.setUserId(user.getId().toString());

        // Validate user status
        validateUserForLogin(user);

        // Validate password
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            user.incrementLoginAttempts();
            userRepository.save(user);
            throw new UnauthorizedException("Credenciales inválidas");
        }

        // Reset login attempts on successful login
        user.resetLoginAttempts();
        user.updateLastLogin();
        user = userRepository.save(user);

        // Create user session
        createUserSession(user, loginRequest);

        // Generate tokens
        String accessToken = jwtService.generateToken(user.getId(), user.getTenantId(), List.of(user.getRole().name()));
        String refreshToken = createRefreshToken(user, loginRequest);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtService.getAccessTokenExpiration() / 1000);

        // Get tenant
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(user.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        log.info("Login successful for user: {} in tenant: {}", user.getEmail(), tenant.getSubdomain());

        return AuthResponseDto.success(
                accessToken,
                refreshToken,
                expiresAt,
                authMapper.toUserDto(user),
                authMapper.toTenantDto(tenant)
        );
    }

    @Transactional
    public AuthResponseDto refreshToken(String refreshTokenValue) {
        log.debug("Refreshing token");

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new UnauthorizedException("Token de refresco inválido"));

        if (!refreshToken.isValid()) {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
            throw new UnauthorizedException("Token de refresco expirado");
        }

        User user = refreshToken.getUser();
        validateUserForLogin(user);

        // Set tenant context
        TenantContext.setTenantId(user.getTenantId().toString());
        TenantContext.setUserId(user.getId().toString());

        // Generate new tokens
        String accessToken = jwtService.generateToken(user.getId(), user.getTenantId(), List.of(user.getRole().name()));
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtService.getAccessTokenExpiration() / 1000);

        // Get tenant
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(user.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        log.debug("Token refreshed successfully for user: {}", user.getEmail());

        return AuthResponseDto.success(
                accessToken,
                refreshTokenValue, // Keep same refresh token
                expiresAt,
                authMapper.toUserDto(user),
                authMapper.toTenantDto(tenant)
        );
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        log.debug("Logging out user");

        String userId = TenantContext.getUserId();
        if (userId != null) {
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            // Revoke refresh token
            if (refreshTokenValue != null) {
                refreshTokenRepository.findByTokenAndRevokedFalse(refreshTokenValue)
                        .ifPresent(token -> {
                            token.revoke();
                            refreshTokenRepository.save(token);
                        });
            }

            // End user session
            userSessionRepository.endAllUserSessions(user, LocalDateTime.now());
            
            log.info("User logged out successfully: {}", user.getEmail());
        }

        // Clear tenant context
        TenantContext.clear();
    }

    @Transactional
    public TenantDto createTenant(CreateTenantRequestDto request) {
        log.info("Creating new tenant: {}", request.getCompanyName());

        // Validate subdomain uniqueness
        if (tenantRepository.existsBySubdomainAndDeletedFalse(request.getSubdomain())) {
            throw new BusinessException("El subdominio ya está en uso");
        }

        // Validate contact email uniqueness
        if (tenantRepository.existsByContactEmailAndDeletedFalse(request.getContactEmail())) {
            throw new BusinessException("El email de contacto ya está registrado");
        }

        // Create tenant
        Tenant tenant = authMapper.toTenant(request);
        tenant.setId(UUID.randomUUID());
        tenant.setTenantId(tenant.getId()); // Set tenant_id to its own id
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setSubscriptionExpiresAt(LocalDate.now().plusDays(30)); // 30-day trial
        tenant = tenantRepository.save(tenant);

        // Create admin user
        CreateUserRequestDto adminRequest = CreateUserRequestDto.builder()
                .email(request.getAdminEmail())
                .password(request.getAdminPassword())
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .phone(request.getAdminPhone())
                .role(RoleType.TENANT_ADMIN)
                .build();

        createUserForTenant(adminRequest, tenant.getId());

        log.info("Tenant created successfully: {} with subdomain: {}", tenant.getCompanyName(), tenant.getSubdomain());

        return authMapper.toTenantDto(tenant);
    }

    @Transactional
    public UserDto createUser(CreateUserRequestDto request) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UnauthorizedException("Contexto de tenant no disponible");
        }

        return createUserForTenant(request, UUID.fromString(tenantId));
    }

    @Transactional
    public UserDto createUserForTenant(CreateUserRequestDto request, UUID tenantId) {
        log.info("Creating new user: {} for tenant: {}", request.getEmail(), tenantId);

        // Validate tenant exists and is active
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        if (!tenant.isActive()) {
            throw new BusinessException("El tenant no está activo");
        }

        // Check user limit
        long currentUserCount = userRepository.countByTenantIdAndDeletedFalse(tenantId);
        if (!tenant.canCreateMoreUsers((int) currentUserCount)) {
            throw new BusinessException("Se ha alcanzado el límite de usuarios para este plan");
        }

        // Validate email uniqueness within tenant
        if (userRepository.existsByEmailAndTenantIdAndDeletedFalse(request.getEmail(), tenantId)) {
            throw new BusinessException("El email ya está registrado en esta empresa");
        }

        // Create user
        User user = authMapper.toUser(request);
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setStatus(UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user = userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getEmailVerificationToken());

        log.info("User created successfully: {} in tenant: {}", user.getEmail(), tenant.getSubdomain());

        return authMapper.toUserDto(user);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserDto> getUsers(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<User> page = userRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return PagedResponse.<UserDto>builder()
                .content(page.getContent().stream().map(authMapper::toUserDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public UserDto updateUser(UUID id, UpdateUserRequestDto request) {
        UUID tenantId = getTenantId();
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        user = userRepository.save(user);
        log.info("User updated: {} in tenant: {}", user.getEmail(), tenantId);
        return authMapper.toUserDto(user);
    }

    @Transactional
    public UserDto updateMyProfile(UpdateMyProfileRequestDto request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UUID tenantId = getTenantId();
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return authMapper.toUserDto(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDto request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (user.isDeleted()) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("La contraseña actual no es correcta");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequestDto request) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(request.getEmail()) && !u.isDeleted())
                .findFirst()
                .orElseThrow(() -> new BusinessException("Si el email está registrado, recibirás instrucciones"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto request) {
        User user = userRepository.findByPasswordResetTokenAndDeletedFalse(request.getToken())
                .orElseThrow(() -> new BusinessException("Token de recuperación inválido"));

        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            user.setPasswordResetToken(null);
            user.setPasswordResetExpiresAt(null);
            userRepository.save(user);
            throw new BusinessException("El token de recuperación ha expirado");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationTokenAndDeletedFalse(token)
                .orElseThrow(() -> new BusinessException("Token de verificación inválido"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    private User findUserByEmail(String email) {
        // In a multi-tenant system, we'd typically need subdomain or tenant context
        // For now, we'll search across all tenants (this should be improved)
        return userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals(email) && !user.isDeleted())
                .findFirst()
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    private void validateUserForLogin(User user) {
        if (user.isDeleted()) {
            throw new UnauthorizedException("Usuario no encontrado");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Usuario inactivo");
        }

        if (user.isLocked()) {
            throw new UnauthorizedException("Cuenta bloqueada temporalmente");
        }

        // Validate tenant
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(user.getTenantId())
                .orElseThrow(() -> new UnauthorizedException("Tenant no encontrado"));

        if (!tenant.isActive()) {
            throw new UnauthorizedException("La empresa no está activa");
        }

        if (!tenant.isSubscriptionValid()) {
            throw new UnauthorizedException("Suscripción expirada");
        }
    }

    private String createRefreshToken(User user, LoginRequestDto loginRequest) {
        // Revoke expired tokens first
        refreshTokenRepository.revokeExpiredTokens(LocalDateTime.now());

        // Enforce session limit: revoke oldest session if at max
        int activeTokens = refreshTokenRepository.countActiveTokensForUser(user);
        if (activeTokens >= 5) {
            List<RefreshToken> active = refreshTokenRepository.findByUserAndRevokedFalse(user);
            active.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
            active.get(0).revoke();
            refreshTokenRepository.save(active.get(0));
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tenantId(user.getTenantId())
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000))
                .deviceInfo(loginRequest.getDeviceInfo())
                .ipAddress(loginRequest.getIpAddress())
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    private void createUserSession(User user, LoginRequestDto loginRequest) {
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID())
                .tenantId(user.getTenantId())
                .sessionId(UUID.randomUUID().toString())
                .user(user)
                .ipAddress(loginRequest.getIpAddress())
                .userAgent(loginRequest.getDeviceInfo())
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        userSessionRepository.save(session);
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}