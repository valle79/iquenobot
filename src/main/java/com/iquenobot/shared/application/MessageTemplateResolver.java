package com.iquenobot.shared.application;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.contact.domain.entity.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reemplaza variables de plantilla (ej. {{business_name}}, {{contact_phone}},
 * {{contact_email}}) en los mensajes salientes del bot con los datos reales
 * del tenant y del contacto.
 */
@Component
@Slf4j
public class MessageTemplateResolver {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z_]+)\\s*}}");

    public String resolve(String message, Tenant tenant, Contact contact) {
        if (message == null || !message.contains("{{")) {
            return message;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            String value = resolveVariable(variable, tenant, contact);

            if (value == null || value.isBlank()) {
                log.warn("Unresolved template variable: {} (leaving placeholder)", variable);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String resolveVariable(String variable, Tenant tenant, Contact contact) {
        return switch (variable) {
            case "business_name", "company_name" ->
                    tenant != null ? firstNonBlank(tenant.getBusinessName(), tenant.getCompanyName()) : null;
            case "contact_phone", "business_phone" -> tenant != null ? tenant.getContactPhone() : null;
            case "contact_email", "business_email" -> tenant != null ? tenant.getContactEmail() : null;
            case "customer_name", "contact_name" -> contact != null ? customerName(contact) : null;
            case "customer_phone" -> contact != null ? contact.getPhone() : null;
            default -> null;
        };
    }

    private String customerName(Contact contact) {
        if (contact.getFullName() != null && !contact.getFullName().isBlank()) {
            return contact.getFullName();
        }
        if (contact.getFirstName() != null && !contact.getFirstName().isBlank()) {
            String lastName = contact.getLastName() != null ? contact.getLastName() : "";
            return (contact.getFirstName() + " " + lastName).trim();
        }
        return contact.getPhone();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
