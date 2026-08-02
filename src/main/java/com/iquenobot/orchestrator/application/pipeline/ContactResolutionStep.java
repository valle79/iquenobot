
package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ContactStatus;
import com.iquenobot.shared.util.PhoneNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContactResolutionStep
        implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ContactRepository contactRepository;

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {

        IncomingMessage message = context.getIncomingMessage();
        UUID tenantId = context.getTenantId();

        String normalizedPhone = PhoneNormalizer.normalize(
                message.getSourceIdentifier()
        );

        if (normalizedPhone == null || !isValidPhone(normalizedPhone)) {
            log.warn(
                    "Ignoring invalid contact identifier. source={} normalized={}",
                    message.getSourceIdentifier(),
                    normalizedPhone
            );
            return context;
        }

        Contact contact = findExistingContact(
                tenantId,
                normalizedPhone,
                message.getChannel()
        ).orElseGet(() -> createContact(tenantId, message, normalizedPhone));

        updateContactNameIfNeeded(contact, message.getSourceName());

        if (!contact.canReceiveMessages()) {
            log.warn(
                    "Contact cannot receive messages: id={} status={}",
                    contact.getId(),
                    contact.getStatus()
            );
        }

        context.setContact(contact);

        TenantContext.setUserId(contact.getId().toString());

        log.debug(
                "Contact resolved: id={} name={} phone={}",
                contact.getId(),
                contact.getDisplayName(),
                contact.getNormalizedPhone()
        );

        return context;
    }

    private Optional<Contact> findExistingContact(
            UUID tenantId,
            String normalizedPhone,
            ChannelType channel
    ) {

        if (channel != ChannelType.WHATSAPP
                && channel != ChannelType.SMS) {
            return Optional.empty();
        }

        log.debug(
                "Searching contact for tenant={} phone={}",
                tenantId,
                normalizedPhone
        );

        Optional<Contact> byNormalized =
                contactRepository.findByNormalizedPhoneAndTenantIdAndDeletedFalse(
                        normalizedPhone,
                        tenantId
                );

        if (byNormalized.isPresent()) {
            return byNormalized;
        }

        Optional<Contact> byPhone =
                contactRepository.findByPhoneAndTenantIdAndDeletedFalse(
                        normalizedPhone,
                        tenantId
                );

        if (byPhone.isPresent()) {
            return byPhone;
        }

        return contactRepository.findByWhatsappPhoneAndTenantIdAndDeletedFalse(
                normalizedPhone,
                tenantId
        );
    }

    private Contact createContact(
            UUID tenantId,
            IncomingMessage message,
            String normalizedPhone
    ) {

        String contactName = sanitizeContactName(
                message.getSourceName(),
                normalizedPhone
        );

        Contact contact = Contact.builder()
                .tenantId(tenantId)
                .fullName(contactName)
                .phone(normalizedPhone)
                .whatsappPhone(normalizedPhone)
                .normalizedPhone(normalizedPhone)
                .status(ContactStatus.ACTIVE)
                .conversationCount(0)
                .messageCount(0)
                .subscribed(true)
                .build();

        contact = contactRepository.save(contact);

        log.info(
                "New contact created: id={} name={} phone={}",
                contact.getId(),
                contact.getFullName(),
                contact.getNormalizedPhone()
        );

        return contact;
    }

    private void updateContactNameIfNeeded(Contact contact, String sourceName) {

        String sanitized = sanitizeContactName(
                sourceName,
                contact.getNormalizedPhone()
        );

        if (sanitized == null || sanitized.equals(contact.getNormalizedPhone())) {
            return;
        }

        String current = contact.getFullName();

        boolean shouldUpdate =
                current == null
                        || current.isBlank()
                        || current.equals(contact.getNormalizedPhone());

        if (shouldUpdate && !sanitized.equals(current)) {

            contact.setFullName(sanitized);

            contactRepository.save(contact);

            log.info(
                    "Updated contact name: id={} newName={}",
                    contact.getId(),
                    sanitized
            );
        }
    }

    private String sanitizeContactName(String sourceName, String normalizedPhone) {

        if (sourceName == null || sourceName.isBlank()) {
            return normalizedPhone;
        }

        String name = sourceName.trim();

        // JIDs de WhatsApp
        if (name.contains("@s.whatsapp.net")
                || name.contains("@g.us")
                || name.contains("@lid")) {
            return normalizedPhone;
        }

        // IDs numéricos largos
        if (name.matches("^\\d{10,}$")) {
            return normalizedPhone;
        }

        // Solo símbolos / emojis
        if (name.matches("^[\\p{Punct}\\p{So}\\s]+$")) {
            return normalizedPhone;
        }

        // Igual al teléfono
        if (name.equals(normalizedPhone)) {
            return normalizedPhone;
        }

        return name;
    }

    private boolean isValidPhone(String phone) {

        if (phone == null) {
            return false;
        }

        return phone.matches("^\\+[1-9]\\d{8,14}$");
    }
}
