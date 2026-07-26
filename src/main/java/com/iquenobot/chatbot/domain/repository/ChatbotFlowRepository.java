package com.iquenobot.chatbot.domain.repository;

import com.iquenobot.chatbot.domain.entity.ChatbotFlow;
import com.iquenobot.shared.enums.ChatbotFlowTrigger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotFlowRepository extends JpaRepository<ChatbotFlow, UUID> {

    Optional<ChatbotFlow> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Page<ChatbotFlow> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<ChatbotFlow> findByTenantIdAndActiveAndDeletedFalseOrderByPriorityDesc(UUID tenantId, boolean active);

    List<ChatbotFlow> findByTenantIdAndTriggerTypeAndActiveAndDeletedFalse(
            UUID tenantId, ChatbotFlowTrigger triggerType, boolean active);

    @Query("SELECT f FROM ChatbotFlow f WHERE f.tenantId = :tenantId AND f.active = true " +
           "AND f.deleted = false AND f.triggerKeywords LIKE %:keyword% ORDER BY f.priority DESC")
    List<ChatbotFlow> findByKeyword(@Param("tenantId") UUID tenantId, @Param("keyword") String keyword);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndActiveAndDeletedFalse(UUID tenantId, boolean active);
}
