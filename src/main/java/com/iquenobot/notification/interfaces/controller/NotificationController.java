package com.iquenobot.notification.interfaces.controller;

import com.iquenobot.notification.application.NotificationService;
import com.iquenobot.notification.domain.dto.CreateNotificationRequestDto;
import com.iquenobot.notification.domain.dto.NotificationDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.enums.NotificationPriority;
import com.iquenobot.shared.enums.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Endpoints para gestión de notificaciones")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{id}")
    @Operation(summary = "Obtener notificación por ID")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<NotificationDto>> getById(@PathVariable UUID id) {
        NotificationDto notification = notificationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Obtener notificaciones de un usuario")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> getAllByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<NotificationDto> notifications = notificationService.getAllByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Obtener notificaciones no leídas de un usuario")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> getUnreadByUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<NotificationDto> notifications = notificationService.getUnreadByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/user/{userId}/type/{type}")
    @Operation(summary = "Obtener notificaciones por tipo")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> getByUserAndType(
            @PathVariable UUID userId,
            @PathVariable NotificationType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<NotificationDto> notifications = notificationService.getByUserAndType(userId, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/user/{userId}/unread/count")
    @Operation(summary = "Obtener contador de notificaciones no leídas")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@PathVariable UUID userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/user/{userId}/unread/count/priority/{priority}")
    @Operation(summary = "Obtener contador de notificaciones no leídas por prioridad")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCountByPriority(
            @PathVariable UUID userId,
            @PathVariable NotificationPriority priority) {
        long count = notificationService.getUnreadCountByPriority(userId, priority);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping
    @Operation(summary = "Crear notificación")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<NotificationDto>> create(@Valid @RequestBody CreateNotificationRequestDto request) {
        NotificationDto notification = notificationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(notification, "Notificación creada exitosamente"));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Marcar notificación como leída")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable UUID id) {
        NotificationDto notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notificación marcada como leída"));
    }

    @PutMapping("/{id}/unread")
    @Operation(summary = "Marcar notificación como no leída")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsUnread(@PathVariable UUID id) {
        NotificationDto notification = notificationService.markAsUnread(id);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notificación marcada como no leída"));
    }

    @PutMapping("/user/{userId}/read-all")
    @Operation(summary = "Marcar todas las notificaciones como leídas")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Todas las notificaciones marcadas como leídas"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar notificación")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificación eliminada exitosamente"));
    }

    @DeleteMapping("/user/{userId}/cleanup")
    @Operation(summary = "Eliminar notificaciones leídas antiguas")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<Void>> deleteOldReadNotifications(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "30") int daysOld) {
        notificationService.deleteOldReadNotifications(userId, daysOld);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificaciones antiguas eliminadas"));
    }

    @PostMapping("/{id}/send")
    @Operation(summary = "Enviar notificación")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendNotification(@PathVariable UUID id) {
        notificationService.sendNotification(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notificación enviada"));
    }
}
