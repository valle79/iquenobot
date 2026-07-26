package com.iquenobot.notification.application;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.notification.domain.dto.CreateNotificationRequestDto;
import com.iquenobot.notification.domain.dto.NotificationDto;
import com.iquenobot.notification.domain.entity.Notification;
import com.iquenobot.notification.domain.repository.NotificationRepository;
import com.iquenobot.notification.interfaces.mapper.NotificationMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public NotificationDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        
        return notificationMapper.toDto(notification);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getAllByUser(UUID userId, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Notification> page = notificationRepository.findByTenantIdAndUserId(tenantId, userId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getUnreadByUser(UUID userId, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Notification> page = notificationRepository.findByTenantIdAndUserIdAndReadFalse(tenantId, userId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getByUserAndType(UUID userId, NotificationType type, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Notification> page = notificationRepository.findByTenantIdAndUserIdAndType(tenantId, userId, type, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        UUID tenantId = getTenantId();
        return notificationRepository.countByTenantIdAndUserIdAndReadFalse(tenantId, userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCountByPriority(UUID userId, NotificationPriority priority) {
        UUID tenantId = getTenantId();
        return notificationRepository.countUnreadByPriority(tenantId, userId, priority);
    }

    @Transactional
    public NotificationDto create(CreateNotificationRequestDto request) {
        UUID tenantId = getTenantId();

        // Validate user exists
        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(request.getUserId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Notification notification = notificationMapper.toEntity(request);
        notification.setId(UUID.randomUUID());
        notification.setTenantId(tenantId);
        notification.setUser(user);

        notification = notificationRepository.save(notification);
        
        log.info("Notification created: {} for user: {}", notification.getId(), request.getUserId());
        
        return notificationMapper.toDto(notification);
    }

    @Transactional
    public NotificationDto markAsRead(UUID id) {
        UUID tenantId = getTenantId();
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        notification.markAsRead();
        notification = notificationRepository.save(notification);
        
        log.info("Notification marked as read: {}", id);
        
        return notificationMapper.toDto(notification);
    }

    @Transactional
    public NotificationDto markAsUnread(UUID id) {
        UUID tenantId = getTenantId();
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        notification.markAsUnread();
        notification = notificationRepository.save(notification);
        
        log.info("Notification marked as unread: {}", id);
        
        return notificationMapper.toDto(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        UUID tenantId = getTenantId();
        Page<Notification> notifications = notificationRepository.findByTenantIdAndUserIdAndReadFalse(
                tenantId, userId, Pageable.unpaged());
        
        notifications.forEach(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
        
        log.info("All notifications marked as read for user: {}", userId);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        notificationRepository.delete(notification);
        
        log.info("Notification deleted: {}", id);
    }

    @Transactional
    public void deleteOldReadNotifications(UUID userId, int daysOld) {
        UUID tenantId = getTenantId();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        
        notificationRepository.deleteByTenantIdAndUserIdAndReadTrueAndCreatedAtBefore(
                tenantId, userId, cutoffDate);
        
        log.info("Old read notifications deleted for user: {} (older than {} days)", userId, daysOld);
    }

    @Transactional(readOnly = true)
    public List<Notification> getPendingNotifications() {
        UUID tenantId = getTenantId();
        return notificationRepository.findPendingNotifications(tenantId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<Notification> getFailedNotificationsForRetry() {
        UUID tenantId = getTenantId();
        return notificationRepository.findFailedNotificationsForRetry(tenantId);
    }

    @Transactional
    public void sendNotification(UUID id) {
        UUID tenantId = getTenantId();
        Notification notification = notificationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        try {
            // Here you would integrate with actual notification services
            // For now, just mark as sent
            notification.markAsSent();
            notification.markAsDelivered();
            notification = notificationRepository.save(notification);
            
            log.info("Notification sent: {}", id);
        } catch (Exception e) {
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
            log.error("Failed to send notification: {}", id, e);
        }
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private PagedResponse<NotificationDto> buildPagedResponse(Page<Notification> page) {
        return PagedResponse.<NotificationDto>builder()
                .content(page.getContent().stream().map(notificationMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
