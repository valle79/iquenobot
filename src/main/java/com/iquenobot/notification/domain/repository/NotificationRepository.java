package com.iquenobot.notification.domain.repository;

import com.iquenobot.notification.domain.entity.Notification;
import com.iquenobot.shared.enums.NotificationChannel;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Notification> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);

    Page<Notification> findByTenantIdAndUserIdAndReadFalse(UUID tenantId, UUID userId, Pageable pageable);

    Page<Notification> findByTenantIdAndUserIdAndType(UUID tenantId, UUID userId, NotificationType type, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.sent = false " +
           "AND n.failed = false AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findPendingNotifications(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.failed = true " +
           "AND n.retryCount < n.maxRetries ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findFailedNotificationsForRetry(@Param("tenantId") UUID tenantId);

    long countByTenantIdAndUserIdAndReadFalse(UUID tenantId, UUID userId);

    long countByTenantIdAndUserId(UUID tenantId, UUID userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.user.id = :userId " +
           "AND n.read = false AND n.priority = :priority")
    long countUnreadByPriority(@Param("tenantId") UUID tenantId, 
                               @Param("userId") UUID userId, 
                               @Param("priority") NotificationPriority priority);

    void deleteByTenantIdAndUserIdAndReadTrueAndCreatedAtBefore(UUID tenantId, UUID userId, LocalDateTime date);
}
