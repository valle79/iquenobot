package com.iquenobot.auth.domain.entity;

import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.RoleType;
import com.iquenobot.shared.enums.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends SoftDeletableEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RoleType role;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "login_attempts", nullable = false)
    private int loginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @ManyToOne
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false)
    private Tenant tenant;

    @OneToMany(mappedBy = "user")
    private Set<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "user")
    private Set<UserSession> userSessions;

    // Business methods
    public boolean isActive() {
        return !isDeleted() && status == UserStatus.ACTIVE;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void lockAccount(int lockoutMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
    }

    public void lockAccount() {
        lockAccount(30);
    }

    public void unlockAccount() {
        this.lockedUntil = null;
        this.loginAttempts = 0;
    }

    public void incrementLoginAttempts(int maxAttempts, int lockoutMinutes) {
        this.loginAttempts++;
        if (this.loginAttempts >= maxAttempts) {
            lockAccount(lockoutMinutes);
        }
    }

    public void incrementLoginAttempts() {
        incrementLoginAttempts(5, 30);
    }

    public void resetLoginAttempts() {
        this.loginAttempts = 0;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}