package com.iquenobot.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.base-url:http://localhost:8085}")
    private String baseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        var securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("IquenoBot CRM API")
                        .description("""
                                API REST del CRM Omnicanal con Inteligencia Artificial.
                                
                                Características:
                                - Multi-tenant (cada empresa aísla sus datos)
                                - Soporte multicanal (WhatsApp, Telegram, Messenger, Instagram, Email)
                                - Chatbot con IA (OpenAI, Groq, Gemini, Claude)
                                - Roles RBAC: SUPER_ADMIN, TENANT_ADMIN, SUPERVISOR, AGENT, BOT
                                - Gestión completa de conversaciones, contactos y productos
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IquenoBot Team")
                                .email("support@iquenobot.com")
                                .url(baseUrl))
                        .license(new License()
                                .name("Proprietary")
                                .url(baseUrl + "/license")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token obtenido del endpoint de login")));
    }
}