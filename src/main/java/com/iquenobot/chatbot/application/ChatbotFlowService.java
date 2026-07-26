package com.iquenobot.chatbot.application;

import com.iquenobot.chatbot.domain.dto.ChatbotFlowDto;
import com.iquenobot.chatbot.domain.dto.CreateChatbotFlowRequestDto;
import com.iquenobot.chatbot.domain.entity.ChatbotFlow;
import com.iquenobot.chatbot.domain.repository.ChatbotFlowRepository;
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
public class ChatbotFlowService {

    private final ChatbotFlowRepository flowRepository;
    private final ChatbotMapper chatbotMapper;

    @Transactional(readOnly = true)
    public ChatbotFlowDto getById(UUID id) {
        UUID tenantId = getTenantId();
        ChatbotFlow flow = flowRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo no encontrado"));
        return chatbotMapper.toFlowDto(flow);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChatbotFlowDto> getAll(Pageable pageable) {
        UUID tenantId = getTenantId();
        Page<ChatbotFlow> page = flowRepository.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return buildPagedResponse(page);
    }

    @Transactional
    public ChatbotFlowDto create(CreateChatbotFlowRequestDto request) {
        UUID tenantId = getTenantId();

        ChatbotFlow flow = chatbotMapper.toFlow(request);
        flow.setId(UUID.randomUUID());
        flow.setTenantId(tenantId);

        flow = flowRepository.save(flow);
        log.info("Chatbot flow created: {} in tenant: {}", flow.getName(), tenantId);
        return chatbotMapper.toFlowDto(flow);
    }

    @Transactional
    public ChatbotFlowDto update(UUID id, CreateChatbotFlowRequestDto request) {
        UUID tenantId = getTenantId();
        ChatbotFlow flow = flowRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo no encontrado"));

        flow.setName(request.getName());
        flow.setDescription(request.getDescription());
        flow.setTriggerType(request.getTriggerType());
        flow.setTriggerKeywords(request.getTriggerKeywords());
        flow.setTriggerPattern(request.getTriggerPattern());
        flow.setFlowConfig(request.getFlowConfig());
        flow.setActive(request.isActive());
        flow.setUseAI(request.isUseAI());
        flow.setAiPrompt(request.getAiPrompt());
        flow.setFallbackMessage(request.getFallbackMessage());
        if (request.getPriority() != null) {
            flow.setPriority(request.getPriority());
        }

        flow = flowRepository.save(flow);
        log.info("Chatbot flow updated: {} in tenant: {}", flow.getName(), tenantId);
        return chatbotMapper.toFlowDto(flow);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        ChatbotFlow flow = flowRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo no encontrado"));

        UUID userId = UUID.fromString(TenantContext.getUserId());
        flow.softDelete(userId);
        flowRepository.save(flow);
        log.info("Chatbot flow deleted: {} in tenant: {}", flow.getName(), tenantId);
    }

    @Transactional(readOnly = true)
    public long getCount() {
        UUID tenantId = getTenantId();
        return flowRepository.countByTenantIdAndDeletedFalse(tenantId);
    }

    @Transactional(readOnly = true)
    public long getActiveCount() {
        UUID tenantId = getTenantId();
        return flowRepository.countByTenantIdAndActiveAndDeletedFalse(tenantId, true);
    }

    private PagedResponse<ChatbotFlowDto> buildPagedResponse(Page<ChatbotFlow> page) {
        return PagedResponse.<ChatbotFlowDto>builder()
                .content(page.getContent().stream().map(chatbotMapper::toFlowDto).toList())
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
