package com.iquenobot.auth.domain.repository;

import com.iquenobot.auth.domain.entity.RefreshToken;
import com.iquenobot.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.user = :user AND rt.revoked = false")
    int revokeAllUserTokens(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.expiresAt < :now AND rt.revoked = false")
    int revokeExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true AND rt.revokedAt < :cutoffDate")
    int deleteOldRevokedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false")
    int countActiveTokensForUser(@Param("user") User user);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.tenantId = :tenantId AND rt.revoked = false")
    List<RefreshToken> findActiveTokensByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.tenantId = :tenantId AND rt.revoked = false")
    long countActiveTokensByTenant(@Param("tenantId") UUID tenantId);
}