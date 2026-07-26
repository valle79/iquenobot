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

import java.time.LocalDateTime;
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

        log.debug("Contact resolved: id={} name={} phone={}",
                contact.getId(), contact.getFullName(), contact.getPhone());
        return context;
    }

    private Optional<Contact> findExistingContact(UUID tenantId, IncomingMessage message) {
        String source = message.getSourceIdentifier();

        if (message.getChannel() == ChannelType.WHATSAPP || message.getChannel() == ChannelType.SMS) {
            Optional<Contact> byPhone = contactRepository
                    .findByPhoneAndTenantIdAndDeletedFalse(source, tenantId);
            if (byPhone.isPresent()) return byPhone;

            Optional<Contact> byWhatsApp = contactRepository
                    .findByWhatsappPhoneAndTenantIdAndDeletedFalse(source, tenantId);
            if (byWhatsApp.isPresent()) return byWhatsApp;
        }

        if (message.getChannel() == ChannelType.EMAIL) {
            Optional<Contact> byEmail = contactRepository
                    .findByEmailAndTenantIdAndDeletedFalse(source, tenantId);
            if (byEmail.isPresent()) return byEmail;
        }

        return contactRepository
                .findByPhoneAndTenantIdAndDeletedFalse(source, tenantId);
    }

    private Contact createContact(UUID tenantId, IncomingMessage message) {
        Contact contact = Contact.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .phone(message.getChannel() == ChannelType.WHATSAPP ? message.getSourceIdentifier() : null)
                .whatsappPhone(message.getChannel() == ChannelType.WHATSAPP ? message.getSourceIdentifier() : null)
                .fullName(message.getSourceName() != null ? message.getSourceName() : message.getSourceIdentifier())
                .status(ContactStatus.ACTIVE)
                .conversationCount(0)
                .messageCount(0)
                .subscribed(true)
                .build();

        contact = contactRepository.save(contact);
        log.info("New contact created: id={} phone={} via channel={}",
                contact.getId(), contact.getPhone(), message.getChannel());
        return contact;
    }

    private static class TenantContextHolder {
        static void setUserId(String userId) {
            com.iquenobot.shared.domain.util.TenantContext.setUserId(userId);
        }
    }
}
