package com.iquenobot.sales.domain.repository;

import com.iquenobot.sales.domain.entity.QuoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuoteHistoryRepository extends JpaRepository<QuoteHistory, UUID> {

    List<QuoteHistory> findByTenantIdAndQuoteIdOrderByCreatedAtAsc(UUID tenantId, UUID quoteId);
}
