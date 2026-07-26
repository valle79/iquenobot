package com.iquenobot.role.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.role.domain.dto.CreateRoleRequestDto;
import com.iquenobot.role.domain.dto.RoleDto;
import com.iquenobot.role.domain.dto.UpdateRoleRequestDto;
import com.iquenobot.role.domain.entity.Role;
import com.iquenobot.role.domain.repository.RoleRepository;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PagedResponse<RoleDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Role> page = roleRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);

        return PagedResponse.<RoleDto>builder()
                .content(page.getContent().stream().map(this::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public RoleDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Role role = findById(id, tenantId);
        return toDto(role);
    }

    @Transactional
    public RoleDto create(CreateRoleRequestDto request) {
        UUID tenantId = getTenantId();

        if (roleRepository.existsByNameAndTenantIdAndDeletedFalse(request.getName(), tenantId)) {
            throw new BusinessException("Ya existe un rol con ese nombre");
        }

        Role role = Role.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(request.getName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .permissions(serializePermissions(request.getPermissions()))
                .system(false)
                .build();

        role = roleRepository.save(role);
        log.info("Role created: {} in tenant: {}", role.getName(), tenantId);
        return toDto(role);
    }

    @Transactional
    public RoleDto update(UUID id, UpdateRoleRequestDto request) {
        UUID tenantId = getTenantId();
        Role role = findById(id, tenantId);

        if (role.isSystem()) {
            throw new BusinessException("No se puede modificar un rol del sistema");
        }

        if (request.getDisplayName() != null) {
            role.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getPermissions() != null) {
            role.setPermissions(serializePermissions(request.getPermissions()));
        }

        role = roleRepository.save(role);
        log.info("Role updated: {} in tenant: {}", role.getName(), tenantId);
        return toDto(role);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        Role role = findById(id, tenantId);

        if (role.isSystem()) {
            throw new BusinessException("No se puede eliminar un rol del sistema");
        }

        String userIdStr = TenantContext.getUserId();
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
        role.softDelete(userId);
        roleRepository.save(role);
        log.info("Role deleted: {} in tenant: {}", role.getName(), tenantId);
    }

    private Role findById(UUID id, UUID tenantId) {
        return roleRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private RoleDto toDto(Role role) {
        int userCount = (int) userRepository.findByTenantIdAndRoleAndDeletedFalse(
                role.getTenantId(),
                com.iquenobot.shared.enums.RoleType.valueOf(role.getName()),
                Pageable.unpaged()).getTotalElements();

        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .permissions(deserializePermissions(role.getPermissions()))
                .userCount(userCount)
                .system(role.isSystem())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    private String serializePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(permissions);
        } catch (JsonProcessingException e) {
            log.error("Error serializing permissions", e);
            return "[]";
        }
    }

    private List<String> deserializePermissions(String permissions) {
        if (permissions == null || permissions.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(permissions, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error deserializing permissions", e);
            return Collections.emptyList();
        }
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
