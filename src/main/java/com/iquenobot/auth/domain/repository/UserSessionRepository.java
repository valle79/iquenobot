package com.iquenobot.auth.domain.repository;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.entity.UserSession;
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
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionIdAndActiveTrue(String sessionId);

    List<UserSession> findByUserAndActiveTrue(User user);

    List<UserSession> findByUserOrderByStartedAtDesc(User user);

    @Query("SELECT us FROM UserSession us WHERE us.user.tenantId = :tenantId AND us.active = true")
    List<UserSession> findActiveSessionsByTenant(@Param("tenantId") UUID tenantId);

    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.endedAt = :now WHERE us.user = :user AND us.active = true")
    int endAllUserSessions(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession us SET us.active = false, us.endedAt = :now WHERE us.lastActivityAt < :cutoffTime AND us.active = true")
    int endExpiredSessions(@Param("cutoffTime") LocalDateTime cutoffTime, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user = :user AND us.active = true")
    int countActiveSessionsForUser(@Param("user") User user);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.tenantId = :tenantId AND us.active = true")
    long countActiveSessionsByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT us FROM UserSession us WHERE us.user.tenantId = :tenantId AND us.startedAt >= :startDate")
    List<UserSession> findSessionsByTenantAndDateRange(@Param("tenantId") UUID tenantId, 
                                                       @Param("startDate") LocalDateTime startDate);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.active = false AND us.endedAt < :cutoffDate")
    int deleteOldInactiveSessions(@Param("cutoffDate") LocalDateTime cutoffDate);
}