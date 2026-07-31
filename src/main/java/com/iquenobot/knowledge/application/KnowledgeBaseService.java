package com.iquenobot.knowledge.application;

import com.iquenobot.knowledge.domain.dto.CreateKnowledgeBaseRequestDto;
import com.iquenobot.knowledge.domain.dto.KnowledgeBaseDto;
import com.iquenobot.knowledge.domain.dto.UpdateKnowledgeBaseRequestDto;
import com.iquenobot.knowledge.domain.entity.KnowledgeBase;
import com.iquenobot.knowledge.domain.repository.KnowledgeBaseRepository;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.product.domain.repository.ProductRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ProductStatus;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository repository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDto> getAll() {
        UUID tenantId = getTenantId();
        return repository.findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseDto getById(UUID id) {
        UUID tenantId = getTenantId();
        KnowledgeBase kb = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Entrada de conocimiento no encontrada"));
        if (!kb.getTenantId().equals(tenantId) || kb.isDeleted()) {
            throw new BusinessException("Entrada de conocimiento no encontrada");
        }
        return toDto(kb);
    }

    @Transactional
    public KnowledgeBaseDto create(CreateKnowledgeBaseRequestDto request) {
        UUID tenantId = getTenantId();
        String userIdStr = TenantContext.getUserId();
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

        KnowledgeBase kb = KnowledgeBase.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .sourceType(request.getSourceType() != null ? request.getSourceType() : "manual")
                .sourceUrl(request.getSourceUrl())
                .fileUrl(request.getFileUrl())
                .tags(request.getTags())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        repository.save(kb);
        log.info("Knowledge base entry created: {} in tenant: {}", kb.getTitle(), tenantId);
        return toDto(kb);
    }

    @Transactional
    public KnowledgeBaseDto update(UUID id, UpdateKnowledgeBaseRequestDto request) {
        UUID tenantId = getTenantId();
        String userIdStr = TenantContext.getUserId();
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

        KnowledgeBase kb = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Entrada de conocimiento no encontrada"));
        if (!kb.getTenantId().equals(tenantId) || kb.isDeleted()) {
            throw new BusinessException("Entrada de conocimiento no encontrada");
        }

        kb.setTitle(request.getTitle().trim());
        kb.setContent(request.getContent().trim());
        kb.setSourceType(request.getSourceType() != null ? request.getSourceType() : "manual");
        kb.setSourceUrl(request.getSourceUrl());
        kb.setFileUrl(request.getFileUrl());
        kb.setTags(request.getTags());
        kb.setUpdatedBy(userId);

        repository.save(kb);
        log.info("Knowledge base entry updated: {} in tenant: {}", kb.getTitle(), tenantId);
        return toDto(kb);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = getTenantId();
        String userIdStr = TenantContext.getUserId();
        UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;

        KnowledgeBase kb = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Entrada de conocimiento no encontrada"));
        if (!kb.getTenantId().equals(tenantId)) {
            throw new BusinessException("Entrada de conocimiento no encontrada");
        }

        kb.softDelete(userId);
        repository.save(kb);
        log.info("Knowledge base entry deleted: {} in tenant: {}", kb.getTitle(), tenantId);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseDto> search(String query) {
        UUID tenantId = getTenantId();
        if (query == null || query.isBlank()) {
            return getAll();
        }
        List<KnowledgeBase> results = repository.searchByText(tenantId, query.trim(), 10);
        if (results.isEmpty()) {
            results = repository.searchByKeyword(tenantId, "%" + query.trim() + "%");
        }
        return results.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public String buildContextForQuery(String userMessage) {
        UUID tenantId = getTenantId();
        StringBuilder sb = new StringBuilder();

        List<KnowledgeBase> results = repository.searchByText(tenantId, userMessage, 5);
        if (results.isEmpty()) {
            results = repository.searchByKeyword(tenantId, "%" + userMessage + "%");
        }
        if (!results.isEmpty()) {
            sb.append("\n\n--- INFORMACIÓN DE LA EMPRESA ---\n");
            for (KnowledgeBase kb : results) {
                sb.append("[").append(kb.getTitle()).append("]\n");
                sb.append(kb.getContent()).append("\n\n");
            }
            sb.append("--- FIN INFORMACIÓN ---\n");
        }

        List<Product> products = findMatchingProducts(tenantId, userMessage);
        if (!products.isEmpty()) {
            sb.append("\n\n--- CATÁLOGO DE PRODUCTOS ---\n");
            for (Product p : products) {
                sb.append("- ").append(p.getName());
                if (p.getSku() != null && !p.getSku().isBlank()) {
                    sb.append(" (SKU: ").append(p.getSku()).append(")");
                }
                sb.append(" | Precio: S/ ").append(p.getPrice());
                if (p.getShortDescription() != null && !p.getShortDescription().isBlank()) {
                    sb.append(" | ").append(p.getShortDescription());
                }
                sb.append("\n");
            }
            sb.append("--- FIN CATÁLOGO ---\n");
        }

        if (sb.length() > 0) {
            sb.append("Usa la información anterior para responder la consulta del cliente.");
            return sb.toString();
        }
        return "";
    }

    private List<Product> findMatchingProducts(UUID tenantId, String message) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        Set<Product> matches = new LinkedHashSet<>();
        for (String token : message.toLowerCase().split("[^a-záéíóúñ0-9]+")) {
            if (token.length() < 3) {
                continue;
            }
            Page<Product> page = productRepository.searchProducts(tenantId, token, PageRequest.of(0, 5));
            for (Product product : page.getContent()) {
                if (product.getStatus() == ProductStatus.ACTIVE) {
                    matches.add(product);
                }
            }
            if (matches.size() >= 10) {
                break;
            }
        }
        return matches.stream().limit(10).toList();
    }

    private KnowledgeBaseDto toDto(KnowledgeBase kb) {
        return KnowledgeBaseDto.builder()
                .id(kb.getId())
                .title(kb.getTitle())
                .content(kb.getContent())
                .sourceType(kb.getSourceType())
                .sourceUrl(kb.getSourceUrl())
                .fileUrl(kb.getFileUrl())
                .tags(kb.getTags())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
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
