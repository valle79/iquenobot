package com.iquenobot.knowledge.domain.repository;

import com.iquenobot.knowledge.domain.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    List<KnowledgeBase> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(UUID tenantId);

    @Query(value = """
            SELECT * FROM knowledge_base
            WHERE tenant_id = :tenantId
              AND is_deleted = false
              AND to_tsvector('spanish',
                  coalesce(content, '') || ' ' ||
                  coalesce(extracted_text, '') || ' ' ||
                  coalesce(title, ''))
                  @@ plainto_tsquery('spanish', :query)
            ORDER BY ts_rank(
                to_tsvector('spanish',
                    coalesce(content, '') || ' ' ||
                    coalesce(extracted_text, '') || ' ' ||
                    coalesce(title, '')),
                plainto_tsquery('spanish', :query)
            ) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeBase> searchByText(@Param("tenantId") UUID tenantId,
                                     @Param("query") String query,
                                     @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM knowledge_base
            WHERE tenant_id = :tenantId
              AND is_deleted = false
              AND (title ILIKE :pattern OR content ILIKE :pattern
                   OR extracted_text ILIKE :pattern OR tags ILIKE :pattern)
            ORDER BY created_at DESC
            LIMIT 50
            """, nativeQuery = true)
    List<KnowledgeBase> searchByKeyword(@Param("tenantId") UUID tenantId,
                                        @Param("pattern") String pattern);
}
