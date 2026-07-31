package com.iquenobot.sales.domain.repository;

import com.iquenobot.sales.domain.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID>, JpaSpecificationExecutor<Quote> {

    Optional<Quote> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
