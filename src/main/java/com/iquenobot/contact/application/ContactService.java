package com.iquenobot.contact.application;

import com.iquenobot.contact.domain.dto.ContactDto;
import com.iquenobot.contact.domain.dto.CreateContactRequestDto;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.contact.interfaces.mapper.ContactMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ContactStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;
    private final ContactMapper contactMapper;

    @Transactional(readOnly = true)
    public ContactDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));
        
        return contactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public ContactDto findByPhone(String phone) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByPhoneAndTenantIdAndDeletedFalse(phone, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado con el teléfono: " + phone));
        
        return contactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public ContactDto findByWhatsAppPhone(String whatsappPhone) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByWhatsappPhoneAndTenantIdAndDeletedFalse(whatsappPhone, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado con WhatsApp: " + whatsappPhone));
        
        return contactMapper.toDto(contact);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Contact> page = contactRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> searchContacts(String search, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Contact> page = contactRepository.searchContacts(tenantId, search, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ContactDto> getByStatus(ContactStatus status, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Contact> page = contactRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional
    public ContactDto create(CreateContactRequestDto request) {
        UUID tenantId = getTenantId();

        // Validate phone uniqueness
        if (request.getPhone() != null && 
            contactRepository.existsByPhoneAndTenantIdAndDeletedFalse(request.getPhone(), tenantId)) {
            throw new BusinessException("Ya existe un contacto con ese número de teléfono");
        }

        // Validate email uniqueness
        if (request.getEmail() != null && 
            contactRepository.existsByEmailAndTenantIdAndDeletedFalse(request.getEmail(), tenantId)) {
            throw new BusinessException("Ya existe un contacto con ese email");
        }

        Contact contact = contactMapper.toEntity(request);
        contact.setId(UUID.randomUUID());
        contact.setTenantId(tenantId);
        contact.updateFullName();
        
        contact = contactRepository.save(contact);
        
        log.info("Contact created: {} for tenant: {}", contact.getId(), tenantId);
        
        return contactMapper.toDto(contact);
    }

    @Transactional
    public ContactDto update(UUID id, CreateContactRequestDto request) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));

        // Validate phone uniqueness (excluding current contact)
        if (request.getPhone() != null && !request.getPhone().equals(contact.getPhone())) {
            if (contactRepository.existsByPhoneAndTenantIdAndDeletedFalse(request.getPhone(), tenantId)) {
                throw new BusinessException("Ya existe un contacto con ese número de teléfono");
            }
        }

        // Validate email uniqueness (excluding current contact)
        if (request.getEmail() != null && !request.getEmail().equals(contact.getEmail())) {
            if (contactRepository.existsByEmailAndTenantIdAndDeletedFalse(request.getEmail(), tenantId)) {
                throw new BusinessException("Ya existe un contacto con ese email");
            }
        }

        // Update fields
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setWhatsappPhone(request.getWhatsappPhone());
        contact.setCompany(request.getCompany());
        contact.setJobTitle(request.getJobTitle());
        contact.setAvatarUrl(request.getAvatarUrl());
        contact.setLanguage(request.getLanguage());
        contact.setTimezone(request.getTimezone());
        contact.setTags(request.getTags());
        contact.setNotes(request.getNotes());
        contact.updateFullName();
        
        contact = contactRepository.save(contact);
        
        log.info("Contact updated: {} for tenant: {}", id, tenantId);
        
        return contactMapper.toDto(contact);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));

        UUID userId = UUID.fromString(TenantContext.getUserId());
        contact.softDelete(userId);
        contactRepository.save(contact);
        
        log.info("Contact soft deleted: {} by user: {}", id, userId);
    }

    @Transactional
    public void blockContact(UUID id, String reason) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));

        contact.block(reason);
        contactRepository.save(contact);
        
        log.info("Contact blocked: {} for tenant: {}", id, tenantId);
    }

    @Transactional
    public void unblockContact(UUID id) {
        UUID tenantId = getTenantId();
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));

        contact.unblock();
        contactRepository.save(contact);
        
        log.info("Contact unblocked: {} for tenant: {}", id, tenantId);
    }

    public Contact findOrCreateByPhone(String phone, String name) {
        UUID tenantId = getTenantId();
        
        return contactRepository.findByPhoneAndTenantIdAndDeletedFalse(phone, tenantId)
                .orElseGet(() -> {
                    Contact newContact = Contact.builder()
                            .id(UUID.randomUUID())
                            .tenantId(tenantId)
                            .phone(phone)
                            .whatsappPhone(phone)
                            .fullName(name)
                            .status(ContactStatus.ACTIVE)
                            .conversationCount(0)
                            .messageCount(0)
                            .subscribed(true)
                            .build();
                    
                    return contactRepository.save(newContact);
                });
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private PagedResponse<ContactDto> buildPagedResponse(Page<Contact> page) {
        return PagedResponse.<ContactDto>builder()
                .content(page.getContent().stream().map(contactMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}