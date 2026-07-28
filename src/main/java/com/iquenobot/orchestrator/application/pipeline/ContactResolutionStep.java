package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.orchestrator.domain.model.IncomingMessage;
import com.iquenobot.shared.enums.ContactStatus;
import com.iquenobot.shared.enums.ChannelType;
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
        String source = normalizePhone(message.getSourceIdentifier());
        ChannelType channel = message.getChannel();

        return switch (channel) {
            case WHATSAPP, SMS -> {
                Optional<Contact> byPhone = contactRepository
                        .findByPhoneAndTenantIdAndDeletedFalse(source, tenantId);
                if (byPhone.isPresent()) yield byPhone;

                yield contactRepository
                        .findByWhatsappPhoneAndTenantIdAndDeletedFalse(source, tenantId);
            }
            case EMAIL -> contactRepository
                    .findByEmailAndTenantIdAndDeletedFalse(source, tenantId);
            default -> {
                Optional<Contact> byPhone = contactRepository
                        .findByPhoneAndTenantIdAndDeletedFalse(source, tenantId);
                if (byPhone.isPresent()) yield byPhone;

                yield Optional.empty();
            }
        };
    }

    private Contact createContact(UUID tenantId, IncomingMessage message) {
        ChannelType channel = message.getChannel();
        String sourceId = normalizePhone(message.getSourceIdentifier());
        String sourceName = message.getSourceName();

        Contact.ContactBuilder<?, ?> builder = Contact.builder()
                .tenantId(tenantId)
                .fullName(sourceName != null && !sourceName.isBlank() ? sourceName : sourceId)
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
                // Social media channels: store identifier in phone field as fallback
                // since Contact has no dedicated social media columns
            }
        }

        Contact contact = contactRepository.save(builder.build());
        log.info("New contact created: id={} channel={} sourceId={}",
                contact.getId(), channel, sourceId);
        return contact;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    private static class TenantContextHolder {
        static void setUserId(String userId) {
            com.iquenobot.shared.domain.util.TenantContext.setUserId(userId);
        }
    }
}
