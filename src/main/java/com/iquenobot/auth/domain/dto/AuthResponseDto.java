package com.iquenobot.auth.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponseDto {

    private String accessToken;
    private String refreshToken;
    private String tokenType;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private UserDto user;
    private TenantDto tenant;

    public static AuthResponseDto success(String accessToken, String refreshToken, 
                                         LocalDateTime expiresAt, UserDto user, TenantDto tenant) {
        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .user(user)
                .tenant(tenant)
                .build();
    }
}