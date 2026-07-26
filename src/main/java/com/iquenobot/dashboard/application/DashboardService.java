package com.iquenobot.dashboard.application;

import com.iquenobot.contact.domain.repository.ContactRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.dashboard.domain.dto.ContactStatsDto;
import com.iquenobot.dashboard.domain.dto.ConversationStatsDto;
import com.iquenobot.dashboard.domain.dto.DashboardOverviewDto;
import com.iquenobot.dashboard.domain.dto.LeadStatsDto;
import com.iquenobot.dashboard.domain.dto.ProductStatsDto;
import com.iquenobot.dashboard.domain.dto.UserActivityDto;
import com.iquenobot.lead.domain.repository.LeadRepository;
import com.iquenobot.product.domain.repository.CategoryRepository;
import com.iquenobot.product.domain.repository.ProductRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.ContactStatus;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.LeadStatus;
import com.iquenobot.shared.enums.ProductStatus;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ConversationRepository conversationRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewDto getOverview() {
        UUID tenantId = getTenantId();
        
        return DashboardOverviewDto.builder()
                .conversationStats(getConversationStats())
                .leadStats(getLeadStats())
                .contactStats(getContactStats())
                .productStats(getProductStats())
                .build();
    }

    @Transactional(readOnly = true)
    public ConversationStatsDto getConversationStats() {
        UUID tenantId = getTenantId();
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = today.minusDays(7);
        LocalDateTime monthStart = today.minusDays(30);

        long total = conversationRepository.countByTenantIdAndDeletedFalse(tenantId);
        long open = conversationRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ConversationStatus.OPEN);
        long closed = conversationRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ConversationStatus.CLOSED);
        long pending = conversationRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ConversationStatus.PENDING);

        return ConversationStatsDto.builder()
                .totalConversations(total)
                .activeConversations(open + pending)
                .openConversations(open)
                .closedConversations(closed)
                .pendingConversations(pending)
                .averageResponseTimeMinutes(BigDecimal.ZERO) // TODO: Calculate from messages
                .averageResolutionTimeHours(BigDecimal.ZERO) // TODO: Calculate from conversations
                .conversationsToday(countConversationsSince(tenantId, today))
                .conversationsThisWeek(countConversationsSince(tenantId, weekStart))
                .conversationsThisMonth(countConversationsSince(tenantId, monthStart))
                .build();
    }

    @Transactional(readOnly = true)
    public LeadStatsDto getLeadStats() {
        UUID tenantId = getTenantId();
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = today.minusDays(7);
        LocalDateTime monthStart = today.minusDays(30);

        long total = leadRepository.countByTenantIdAndDeletedFalse(tenantId);
        long newLeads = leadRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, LeadStatus.NEW);
        long contacted = leadRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, LeadStatus.CONTACTED);
        long qualified = leadRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, LeadStatus.QUALIFIED);
        long converted = leadRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, LeadStatus.CONVERTED);
        long lost = leadRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, LeadStatus.LOST);

        BigDecimal conversionRate = total > 0 
                ? BigDecimal.valueOf(converted).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return LeadStatsDto.builder()
                .totalLeads(total)
                .newLeads(newLeads)
                .contactedLeads(contacted)
                .qualifiedLeads(qualified)
                .convertedLeads(converted)
                .lostLeads(lost)
                .conversionRate(conversionRate)
                .averageLeadScore(BigDecimal.valueOf(50)) // TODO: Calculate average
                .leadsToday(countLeadsSince(tenantId, today))
                .leadsThisWeek(countLeadsSince(tenantId, weekStart))
                .leadsThisMonth(countLeadsSince(tenantId, monthStart))
                .unassignedLeads(leadRepository.findUnassignedLeads(tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .highScoreLeads(leadRepository.findHighScoreLeads(tenantId, 70).size())
                .build();
    }

    @Transactional(readOnly = true)
    public ContactStatsDto getContactStats() {
        UUID tenantId = getTenantId();
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime weekStart = today.minusDays(7);
        LocalDateTime monthStart = today.minusDays(30);

        long total = contactRepository.countByTenantIdAndDeletedFalse(tenantId);
        long active = contactRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ContactStatus.ACTIVE);
        long blocked = contactRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ContactStatus.BLOCKED);

        return ContactStatsDto.builder()
                .totalContacts(total)
                .activeContacts(active)
                .blockedContacts(blocked)
                .subscribedContacts(0L) // TODO: Add query
                .contactsToday(countContactsSince(tenantId, today))
                .contactsThisWeek(countContactsSince(tenantId, weekStart))
                .contactsThisMonth(countContactsSince(tenantId, monthStart))
                .contactsWithConversations(0L) // TODO: Add query
                .build();
    }

    @Transactional(readOnly = true)
    public ProductStatsDto getProductStats() {
        UUID tenantId = getTenantId();

        long total = productRepository.countByTenantIdAndDeletedFalse(tenantId);
        long active = productRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProductStatus.ACTIVE);
        long outOfStock = productRepository.countByTenantIdAndStatusAndDeletedFalse(tenantId, ProductStatus.OUT_OF_STOCK);
        long lowStock = productRepository.findLowStockProducts(tenantId).size();
        long featured = productRepository.findFeaturedProducts(tenantId).size();
        long categories = categoryRepository.countByTenantIdAndDeletedFalse(tenantId);

        return ProductStatsDto.builder()
                .totalProducts(total)
                .activeProducts(active)
                .outOfStockProducts(outOfStock)
                .lowStockProducts(lowStock)
                .featuredProducts(featured)
                .totalCategories(categories)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserActivityDto> getUserActivity() {
        UUID tenantId = getTenantId();
        // TODO: Implement user activity queries
        return List.of();
    }

    private long countConversationsSince(UUID tenantId, LocalDateTime since) {
        // TODO: Add query to repository
        return 0L;
    }

    private long countLeadsSince(UUID tenantId, LocalDateTime since) {
        // TODO: Add query to repository
        return 0L;
    }

    private long countContactsSince(UUID tenantId, LocalDateTime since) {
        // TODO: Add query to repository
        return 0L;
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
