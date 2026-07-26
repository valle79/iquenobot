package com.iquenobot.auth.interfaces.mapper;

import com.iquenobot.auth.domain.dto.CreateTenantRequestDto;
import com.iquenobot.auth.domain.dto.CreateUserRequestDto;
import com.iquenobot.auth.domain.dto.TenantDto;
import com.iquenobot.auth.domain.dto.UserDto;
import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AuthMapper {

    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "mustChangePassword", source = "mustChangePassword")
    UserDto toUserDto(User user);

    @Mapping(target = "plan", ignore = true)
    TenantDto toTenantDto(Tenant tenant);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "status", constant = "PENDING_VERIFICATION")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "loginAttempts", constant = "0")
    @Mapping(target = "emailVerificationToken", ignore = true)
    @Mapping(target = "passwordResetToken", ignore = true)
    @Mapping(target = "passwordResetExpiresAt", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "userSessions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    User toUser(CreateUserRequestDto createUserRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", expression = "java(java.util.UUID.fromString(\"00000000-0000-0000-0000-000000000000\"))")
    @Mapping(target = "status", constant = "TRIAL")
    @Mapping(target = "users", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Tenant toTenant(CreateTenantRequestDto createTenantRequestDto);
}