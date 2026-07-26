package com.iquenobot.chatbot.domain.repository;

import com.iquenobot.chatbot.domain.entity.ChatbotIntent;
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
public interface ChatbotIntentRepository extends JpaRepository<ChatbotIntent, UUID> {

    Optional<ChatbotIntent> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<ChatbotIntent> findByIntentNameAndTenantIdAndDeletedFalse(String intentName, UUID tenantId);

    Page<ChatbotIntent> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<ChatbotIntent> findByTenantIdAndActiveAndDeletedFalseOrderByPriorityDesc(UUID tenantId, boolean active);

    @Query("SELECT i FROM ChatbotIntent i WHERE i.tenantId = :tenantId AND i.active = true " +
           "AND i.deleted = false AND i.trainingPhrases LIKE %:phrase% ORDER BY i.priority DESC")
    List<ChatbotIntent> findByTrainingPhrase(@Param("tenantId") UUID tenantId, @Param("phrase") String phrase);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndActiveAndDeletedFalse(UUID tenantId, boolean active);

    boolean existsByIntentNameAndTenantIdAndDeletedFalse(String intentName, UUID tenantId);
}
