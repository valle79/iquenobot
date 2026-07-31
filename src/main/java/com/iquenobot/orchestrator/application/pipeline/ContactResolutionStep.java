package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.shared.enums.ContactStatus;
import com.iquenobot.shared.enums.ChannelType;
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
public class ContactResolutionStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private final ContactRepository contactRepository;

    @Override
    public int getOrder() { return 20; }

    @Override
    @Transactional
    public ProcessingContext execute(ProcessingContext context) {
        var message = context.getIncomingMessage();
        UUID tenantId = context.getTenantId();

        Contact contact = findExistingContact(tenantId, message)
                .orElseGet(() -> createContact(tenantId, message));

        if (!contact.canReceiveMessages()) {
            log.warn("Contact cannot receive messages: id={} status={}",
                    contact.getId(), contact.getStatus());
        }

        context.setContact(contact);
        TenantContextHolder.setUserId(contact.getId().toString());

        log.debug("Contact resolved: id={} name={}",
                contact.getId(), contact.getDisplayName());
        return context;
    }

private Optional<Contact> findExistingContact(UUID tenantId, IncomingMessage message) {

    String original = message.getSourceIdentifier();
    String source = PhoneNormalizer.normalize(original);
    ChannelType channel = message.getChannel();

    log.warn("=== CONTACT SEARCH DEBUG ===");
    log.warn("Original source: [{}]", original);
    log.warn("Normalized source: [{}]", source);
    log.warn("Tenant: [{}]", tenantId);
    log.warn("Channel: [{}]", channel);

    return switch (channel) {

        case WHATSAPP, SMS -> {

            Optional<Contact> byNormalized = contactRepository
                    .findByNormalizedPhoneAndTenantIdAndDeletedFalse(source, tenantId);

            log.warn("Search normalized_phone={} found={}",
                    source, byNormalized.isPresent());

            if (byNormalized.isPresent()) {
                Contact c = byNormalized.get();
                log.warn("FOUND by normalized_phone - id={} name={} phone={} normalized={}",
                        c.getId(), c.getFullName(), c.getPhone(), c.getNormalizedPhone());
                yield byNormalized;
            }

            Optional<Contact> byPhone = contactRepository
                    .findByPhoneAndTenantIdAndDeletedFalse(source, tenantId);

            log.warn("Search phone={} found={}",
                    source, byPhone.isPresent());

            if (byPhone.isPresent()) {
                Contact c = byPhone.get();
                log.warn("FOUND by phone - id={} name={}",
                        c.getId(), c.getFullName());
                yield byPhone;
            }

            Optional<Contact> byWhatsapp = contactRepository
                    .findByWhatsappPhoneAndTenantIdAndDeletedFalse(source, tenantId);

            log.warn("Search whatsapp_phone={} found={}",
                    source, byWhatsapp.isPresent());

            if (byWhatsapp.isPresent()) {
                Contact c = byWhatsapp.get();
                log.warn("FOUND by whatsapp_phone - id={} name={}",
                        c.getId(), c.getFullName());
            }

            yield byWhatsapp;
        }

        case EMAIL -> {
            Optional<Contact> byEmail = contactRepository
                    .findByEmailAndTenantIdAndDeletedFalse(source, tenantId);

            log.warn("Search email={} found={}", source, byEmail.isPresent());
            yield byEmail;
        }

        default -> Optional.empty();
    };
}

private Contact createContact(UUID tenantId, IncomingMessage message) {

    ChannelType channel = message.getChannel();
    String sourceId = PhoneNormalizer.normalize(message.getSourceIdentifier());
    String sourceName = sanitizeContactName(message.getSourceName(), sourceId);

    Contact.ContactBuilder<?, ?> builder = Contact.builder()
            .tenantId(tenantId)
            .fullName(sourceName)
            .normalizedPhone(sourceId)
            .status(ContactStatus.ACTIVE)
            .conversationCount(0)
            .messageCount(0)
            .subscribed(true);

    switch (channel) {
        case WHATSAPP -> {
            builder.phone(sourceId);
            builder.whatsappPhone(sourceId);
        }
        case SMS -> builder.phone(sourceId);
        case EMAIL -> builder.email(sourceId);
        default -> {
            // otros canales
        }
    }

    Contact contact = contactRepository.save(builder.build());

    log.info("New contact created: id={} channel={} sourceId={} name={}",
            contact.getId(), channel, sourceId, contact.getFullName());

    return contact;
}


private String sanitizeContactName(String sourceName, String sourceId) {

    if (sourceName == null || sourceName.isBlank()) {
        return sourceId;
    }

    String name = sourceName.trim();

    // IDs numéricos largos (grupos, LID, JID, etc.) → usar teléfono
    if (name.matches("^\\\\d{10,}$")) {
        return sourceId;
    }

    // JIDs de WhatsApp
    if (name.contains("@g.us") || name.contains("@s.whatsapp.net")) {
        return sourceId;
    }

    // Solo emojis o símbolos raros
    if (name.matches("^[\\\\p{So}\\\\p{Cntrl}\\\\s]+$")) {
        return sourceId;
    }

    // Igual al número telefónico
    if (name.equals(sourceId)) {
        return sourceId;
    }

    return name;
}

    private static class TenantContextHolder {
        static void setUserId(String userId) {
            com.iquenobot.shared.domain.util.TenantContext.setUserId(userId);
        }
    }
}
