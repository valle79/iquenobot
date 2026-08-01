package com.iquenobot.auth.application;

import com.iquenobot.auth.domain.dto.*;
import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.RefreshTokenRepository;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.auth.domain.repository.UserSessionRepository;
import com.iquenobot.auth.interfaces.mapper.AuthMapper;
import com.iquenobot.security.application.JwtService;
import com.iquenobot.shared.application.SystemSettingsService;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.TenantStatus;
import com.iquenobot.shared.enums.UserStatus;
import com.iquenobot.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserSessionRepository userSessionRepository;
    @Mock
    private AuthMapper authMapper;
    @Mock
    private JwtService jwtService;
    @Mock
    private SystemSettingsService systemSettingsService;

    @InjectMocks
    private AuthService authService;

    private PasswordEncoder passwordEncoder;
    private User testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .email("user@test.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .role(RoleType.AGENT)
                .emailVerified(false)
                .loginAttempts(0)
                .build();
    }

    @Test
    void forgotPassword_shouldGenerateToken_whenUserExists() {
        when(userRepository.findAll()).thenReturn(java.util.List.of(testUser));

        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("user@test.com");

        authService.forgotPassword(request);

        assertNotNull(testUser.getPasswordResetToken());
        assertNotNull(testUser.getPasswordResetExpiresAt());
        verify(userRepository).save(testUser);
    }

    @Test
    void forgotPassword_shouldNotThrow_whenEmailNotExists() {
        when(userRepository.findAll()).thenReturn(java.util.List.of());

        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("nonexistent@test.com");

        assertDoesNotThrow(() -> authService.forgotPassword(request));
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
        String newPassword = "newPassword123";
        String resetToken = UUID.randomUUID().toString();
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetTokenAndDeletedFalse(resetToken))
                .thenReturn(Optional.of(testUser));

        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setToken(resetToken);
        request.setNewPassword(newPassword);

        authService.resetPassword(request);

        assertTrue(passwordEncoder.matches(newPassword, testUser.getPassword()));
        assertNull(testUser.getPasswordResetToken());
        assertNull(testUser.getPasswordResetExpiresAt());
        verify(userRepository).save(testUser);
    }

    @Test
    void resetPassword_shouldThrow_whenTokenIsExpired() {
        String resetToken = UUID.randomUUID().toString();
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetExpiresAt(LocalDateTime.now().minusHours(1));

        when(userRepository.findByPasswordResetTokenAndDeletedFalse(resetToken))
                .thenReturn(Optional.of(testUser));

        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setToken(resetToken);
        request.setNewPassword("newPassword");

        assertThrows(BusinessException.class, () -> authService.resetPassword(request));
    }

    @Test
    void verifyEmail_shouldMarkAsVerified_whenTokenIsValid() {
        String verificationToken = UUID.randomUUID().toString();
        testUser.setEmailVerificationToken(verificationToken);
        testUser.setEmailVerified(false);

        when(userRepository.findByEmailVerificationTokenAndDeletedFalse(verificationToken))
                .thenReturn(Optional.of(testUser));

        authService.verifyEmail(verificationToken);

        assertTrue(testUser.isEmailVerified());
        assertNull(testUser.getEmailVerificationToken());
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyEmail_shouldThrow_whenTokenIsInvalid() {
        when(userRepository.findByEmailVerificationTokenAndDeletedFalse("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.verifyEmail("invalid-token"));
    }
}
