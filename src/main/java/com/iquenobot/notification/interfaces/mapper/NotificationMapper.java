package com.iquenobot.notification.interfaces.mapper;

import com.iquenobot.notification.domain.dto.CreateNotificationRequestDto;
import com.iquenobot.notification.domain.dto.NotificationDto;
import com.iquenobot.notification.domain.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "sent", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "delivered", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "failed", ignore = true)
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "maxRetries", constant = "3")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Notification toEntity(CreateNotificationRequestDto dto);
}
