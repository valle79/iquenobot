package com.iquenobot.lead.application;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.lead.domain.dto.CreateLeadRequestDto;
import com.iquenobot.lead.domain.dto.LeadDto;
import com.iquenobot.lead.domain.entity.Lead;
import com.iquenobot.lead.domain.repository.LeadRepository;
import com.iquenobot.lead.interfaces.mapper.LeadMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.LeadSource;
import com.iquenobot.shared.enums.LeadStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;

    @Transactional(readOnly = true)
    public LeadDto getById(UUID id) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));
        
        return leadMapper.toDto(lead);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Lead> page = leadRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadDto> getByStatus(LeadStatus status, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Lead> page = leadRepository.findByTenantIdAndStatusAndDeletedFalse(tenantId, status, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadDto> getBySource(LeadSource source, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Lead> page = leadRepository.findByTenantIdAndSourceAndDeletedFalse(tenantId, source, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadDto> getByAssignedUser(UUID userId, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Lead> page = leadRepository.findByTenantIdAndAssignedToIdAndDeletedFalse(tenantId, userId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadDto> getUnassignedLeads(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Lead> page = leadRepository.findUnassignedLeads(tenantId, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public List<LeadDto> getHighScoreLeads(Integer minScore) {
        UUID tenantId = getTenantId();
        List<Lead> leads = leadRepository.findHighScoreLeads(tenantId, minScore != null ? minScore : 70);
        
        return leads.stream().map(leadMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadDto> getStaleLeads(Integer days) {
        UUID tenantId = getTenantId();
        LocalDateTime date = LocalDateTime.now().minusDays(days != null ? days : 7);
        List<Lead> leads = leadRepository.findStaleLeads(tenantId, date);
        
        return leads.stream().map(leadMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<LeadDto> searchLeads(String search, Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<Lead> page = leadRepository.searchLeads(tenantId, search, pageable);
        
        return buildPagedResponse(page);
    }

    @Transactional
    public LeadDto create(CreateLeadRequestDto request) {
        UUID tenantId = getTenantId();

        // Validate contact exists
        Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(request.getContactId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));

        Lead lead = leadMapper.toEntity(request);
        lead.setId(UUID.randomUUID());
        lead.setTenantId(tenantId);
        lead.setContact(contact);

        // Assign to user if provided
        if (request.getAssignedToUserId() != null) {
            User user = userRepository.findByIdAndTenantIdAndDeletedFalse(request.getAssignedToUserId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
            lead.assignToUser(user);
        }

        lead = leadRepository.save(lead);
        
        log.info("Lead created: {} for tenant: {}", lead.getId(), tenantId);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto update(UUID id, CreateLeadRequestDto request) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        // Update fields
        lead.setTitle(request.getTitle());
        lead.setDescription(request.getDescription());
        lead.setStatus(request.getStatus());
        lead.setSource(request.getSource());
        lead.setSourceDetails(request.getSourceDetails());
        lead.setEstimatedValue(request.getEstimatedValue());
        lead.setProbability(request.getProbability());
        lead.setScore(request.getScore());
        lead.setExpectedCloseDate(request.getExpectedCloseDate());
        lead.setTags(request.getTags());
        lead.setNotes(request.getNotes());

        // Update contact if changed
        if (!request.getContactId().equals(lead.getContact().getId())) {
            Contact contact = contactRepository.findByIdAndTenantIdAndDeletedFalse(request.getContactId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contacto no encontrado"));
            lead.setContact(contact);
        }

        lead = leadRepository.save(lead);
        
        log.info("Lead updated: {} for tenant: {}", id, tenantId);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto assignToUser(UUID id, UUID userId) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        User user = userRepository.findByIdAndTenantIdAndDeletedFalse(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        lead.assignToUser(user);
        lead = leadRepository.save(lead);
        
        log.info("Lead {} assigned to user: {}", id, userId);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto markAsContacted(UUID id) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        lead.markAsContacted();
        lead = leadRepository.save(lead);
        
        log.info("Lead marked as contacted: {}", id);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto markAsQualified(UUID id, Integer score) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        lead.markAsQualified(score);
        lead = leadRepository.save(lead);
        
        log.info("Lead marked as qualified: {} with score: {}", id, score);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto markAsConverted(UUID id, String contactId) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        lead.markAsConverted(contactId);
        lead = leadRepository.save(lead);
        
        log.info("Lead marked as converted: {}", id);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto markAsLost(UUID id, String reason) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        lead.markAsLost(reason);
        lead = leadRepository.save(lead);
        
        log.info("Lead marked as lost: {} - reason: {}", id, reason);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto markAsDisqualified(UUID id, String reason) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        lead.markAsDisqualified(reason);
        lead = leadRepository.save(lead);
        
        log.info("Lead marked as disqualified: {} - reason: {}", id, reason);
        
        return leadMapper.toDto(lead);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        Lead lead = leadRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead no encontrado"));

        UUID userId = UUID.fromString(TenantContext.getUserId());
        lead.softDelete(userId);
        leadRepository.save(lead);
        
        log.info("Lead soft deleted: {} by user: {}", id, userId);
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }

    private PagedResponse<LeadDto> buildPagedResponse(Page<Lead> page) {
        return PagedResponse.<LeadDto>builder()
                .content(page.getContent().stream().map(leadMapper::toDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
