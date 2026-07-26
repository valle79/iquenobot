package com.iquenobot.auth.interfaces.controller;

import com.iquenobot.auth.application.AuthService;
import com.iquenobot.auth.domain.dto.AuthResponseDto;
import com.iquenobot.auth.domain.dto.CreateTenantRequestDto;
import com.iquenobot.auth.domain.dto.ChangePasswordRequestDto;
import com.iquenobot.auth.domain.dto.CreateUserRequestDto;
import com.iquenobot.auth.domain.dto.ForgotPasswordRequestDto;
import com.iquenobot.auth.domain.dto.LoginRequestDto;
import com.iquenobot.auth.domain.dto.ResetPasswordRequestDto;
import com.iquenobot.auth.domain.dto.TenantDto;
import com.iquenobot.auth.domain.dto.UpdateMyProfileRequestDto;
import com.iquenobot.auth.domain.dto.UpdateUserRequestDto;
import com.iquenobot.auth.domain.dto.UserDto;
import com.iquenobot.shared.domain.dto.ApiResponse;
import com.iquenobot.shared.domain.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de autenticación y gestión de usuarios")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario y devuelve tokens de acceso y refresco"
    )
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            HttpServletRequest request) {
        
        // Enrich request with IP and device info
        loginRequest.setIpAddress(getClientIpAddress(request));
        if (loginRequest.getDeviceInfo() == null) {
            loginRequest.setDeviceInfo(request.getHeader("User-Agent"));
        }

        AuthResponseDto authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login exitoso"));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar token de acceso",
            description = "Renueva el token de acceso usando el refresh token"
    )
    public ResponseEntity<ApiResponse<AuthResponseDto>> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        
        // Remove "Bearer " prefix if present
        String token = refreshToken.startsWith("Bearer ") ? 
                       refreshToken.substring(7) : refreshToken;

        AuthResponseDto authResponse = authService.refreshToken(token);
        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token renovado exitosamente"));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca el refresh token y termina la sesión del usuario"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        
        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout exitoso"));
    }

    @PostMapping("/tenants")
    @Operation(
            summary = "Crear tenant",
            description = "Crea un nuevo tenant (empresa) con su usuario administrador"
    )
    public ResponseEntity<ApiResponse<TenantDto>> createTenant(
            @Valid @RequestBody CreateTenantRequestDto request) {
        
        TenantDto tenant = authService.createTenant(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tenant, "Tenant creado exitosamente"));
    }

    @PostMapping("/users")
    @Operation(
            summary = "Crear usuario",
            description = "Crea un nuevo usuario en el tenant actual"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequestDto request) {
        
        UserDto user = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "Usuario creado exitosamente"));
    }

    @GetMapping("/users")
    @Operation(
            summary = "Listar usuarios",
            description = "Obtiene la lista paginada de usuarios del tenant actual"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR', 'AGENT')")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<UserDto> users = authService.getUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}")
    @Operation(
            summary = "Actualizar usuario",
            description = "Actualiza los datos de un usuario (nombre, rol, estado, etc.)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        UserDto user = authService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "Usuario actualizado exitosamente"));
    }

    @PutMapping("/users/me")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza los datos del perfil del usuario autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<UserDto>> updateMyProfile(
            @Valid @RequestBody UpdateMyProfileRequestDto request) {
        UserDto user = authService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success(user, "Perfil actualizado exitosamente"));
    }

    @PutMapping("/users/me/password")
    @Operation(
            summary = "Cambiar mi contraseña",
            description = "Cambia la contraseña del usuario autenticado"
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña actualizada exitosamente"));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Genera un token de recuperación y lo devuelve para restablecer la contraseña"
    )
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDto request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Si el email está registrado, recibirás instrucciones"));
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Restablecer contraseña",
            description = "Restablece la contraseña usando el token de recuperación"
    )
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDto request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Contraseña restablecida exitosamente"));
    }

    @PostMapping("/verify-email")
    @Operation(
            summary = "Verificar email",
            description = "Verifica el email del usuario usando el token de verificación"
    )
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verificado exitosamente"));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}