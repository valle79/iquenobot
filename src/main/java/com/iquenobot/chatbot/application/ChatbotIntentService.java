package com.iquenobot.chatbot.application;

import com.iquenobot.chatbot.domain.dto.ChatbotIntentDto;
import com.iquenobot.chatbot.domain.dto.CreateChatbotIntentRequestDto;
import com.iquenobot.chatbot.domain.entity.ChatbotIntent;
import com.iquenobot.chatbot.domain.repository.ChatbotIntentRepository;
import com.iquenobot.chatbot.interfaces.mapper.ChatbotMapper;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.util.TenantContext;
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
public class ChatbotIntentService {

    private final ChatbotIntentRepository intentRepository;
    private final ChatbotMapper chatbotMapper;

    @Transactional(readOnly = true)
    public ChatbotIntentDto getById(UUID id) {
        UUID tenantId = getTenantId();
        ChatbotIntent intent = intentRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Intención no encontrada"));
        return chatbotMapper.toIntentDto(intent);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChatbotIntentDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<ChatbotIntent> page = intentRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return buildPagedResponse(page);
    }

    @Transactional
    public ChatbotIntentDto create(CreateChatbotIntentRequestDto request) {
        UUID tenantId = getTenantId();

        if (intentRepository.existsByIntentNameAndTenantIdAndDeletedFalse(request.getIntentName(), tenantId)) {
            throw new BusinessException("Ya existe una intención con ese nombre");
        }

        ChatbotIntent intent = chatbotMapper.toIntent(request);
        intent.setId(UUID.randomUUID());
        intent.setTenantId(tenantId);

        intent = intentRepository.save(intent);
        log.info("Chatbot intent created: {} in tenant: {}", intent.getIntentName(), tenantId);
        return chatbotMapper.toIntentDto(intent);
    }

    @Transactional
    public ChatbotIntentDto update(UUID id, CreateChatbotIntentRequestDto request) {
        UUID tenantId = getTenantId();
        ChatbotIntent intent = intentRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Intención no encontrada"));

        intent.setIntentName(request.getIntentName());
        intent.setDescription(request.getDescription());
        intent.setTrainingPhrases(request.getTrainingPhrases());
        intent.setResponses(request.getResponses());
        intent.setEntities(request.getEntities());
        intent.setContextRequired(request.getContextRequired());
        intent.setContextOutput(request.getContextOutput());
        intent.setActions(request.getActions());
        intent.setConfidenceThreshold(request.getConfidenceThreshold());
        intent.setActive(request.isActive());
        if (request.getPriority() != null) {
            intent.setPriority(request.getPriority());
        }

        intent = intentRepository.save(intent);
        log.info("Chatbot intent updated: {} in tenant: {}", intent.getIntentName(), tenantId);
        return chatbotMapper.toIntentDto(intent);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        ChatbotIntent intent = intentRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Intención no encontrada"));

        UUID userId = UUID.fromString(TenantContext.getUserId());
        intent.softDelete(userId);
        intentRepository.save(intent);
        log.info("Chatbot intent deleted: {} in tenant: {}", intent.getIntentName(), tenantId);
    }

    @Transactional(readOnly = true)
    public long getCount() {
        UUID tenantId = getTenantId();
        return intentRepository.countByTenantIdAndDeletedFalse(tenantId);
    }

    @Transactional(readOnly = true)
    public long getActiveCount() {
        UUID tenantId = getTenantId();
        return intentRepository.countByTenantIdAndActiveAndDeletedFalse(tenantId, true);
    }

    private PagedResponse<ChatbotIntentDto> buildPagedResponse(Page<ChatbotIntent> page) {
        return PagedResponse.<ChatbotIntentDto>builder()
                .content(page.getContent().stream().map(chatbotMapper::toIntentDto).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
