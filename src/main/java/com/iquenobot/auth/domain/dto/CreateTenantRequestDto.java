package com.iquenobot.auth.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequestDto {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 200)
    private String companyName;

    @Size(max = 200)
    private String businessName;

    @Size(max = 20)
    private String ruc;

    @NotBlank(message = "El subdominio es obligatorio")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]*[a-z0-9]$",
             message = "El subdominio solo puede contener letras minúsculas, números y guiones")
    private String subdomain;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email
    @Size(max = 255)
    private String contactEmail;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    @Size(max = 20)
    private String contactPhone;

    @Size(max = 500)
    private String websiteUrl;

    private String logoUrl;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String country;

    @Size(max = 50)
    private String timezone;

    @Size(max = 5)
    private String currency;

    @Size(max = 10)
    private String language;

    @Size(max = 10)
    private String locale;

    @Size(min = 4, max = 7)
    private String primaryColor;

    @Size(min = 4, max = 7)
    private String secondaryColor;

    private String subscriptionPlan;
    private Integer maxUsers;
    private Integer maxConversations;

    @Min(value = 0, message = "El límite de agentes no puede ser negativo")
    private Integer maxAgents;

    @Min(value = 0, message = "El límite de supervisores no puede ser negativo")
    private Integer maxSupervisors;

    @NotBlank(message = "El email del administrador es obligatorio")
    @Email
    private String adminEmail;

    @NotBlank(message = "La contraseña del administrador es obligatoria")
    @Size(min = 8, max = 100)
    private String adminPassword;

    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(min = 2, max = 100)
    private String adminFirstName;

    @NotBlank(message = "El apellido del administrador es obligatorio")
    @Size(min = 2, max = 100)
    private String adminLastName;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$")
    private String adminPhone;
}
