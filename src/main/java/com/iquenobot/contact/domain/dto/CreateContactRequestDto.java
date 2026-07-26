package com.iquenobot.contact.domain.dto;

import jakarta.validation.constraints.Email;
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
public class CreateContactRequestDto {

    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String firstName;

    @Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    private String lastName;

    @Email(message = "Formato de email inválido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Formato de teléfono inválido")
    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    private String phone;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Formato de teléfono WhatsApp inválido")
    @Size(max = 20, message = "El teléfono WhatsApp no puede exceder 20 caracteres")
    private String whatsappPhone;

    @Size(max = 200, message = "La empresa no puede exceder 200 caracteres")
    private String company;

    @Size(max = 100, message = "El cargo no puede exceder 100 caracteres")
    private String jobTitle;

    @Size(max = 500, message = "La URL del avatar no puede exceder 500 caracteres")
    private String avatarUrl;

    @Size(max = 5, message = "El idioma no puede exceder 5 caracteres")
    private String language;

    @Size(max = 50, message = "La zona horaria no puede exceder 50 caracteres")
    private String timezone;

    private String tags;

    @Size(max = 2000, message = "Las notas no pueden exceder 2000 caracteres")
    private String notes;
}