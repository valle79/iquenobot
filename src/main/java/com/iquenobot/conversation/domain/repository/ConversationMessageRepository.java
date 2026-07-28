package com.iquenobot.conversation.domain.repository;

import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    boolean existsByChannelMessageId(String channelMessageId);

    Optional<ConversationMessage> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ConversationMessage> findByChannelMessageIdAndTenantId(String channelMessageId, UUID tenantId);

    Page<ConversationMessage> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);

    List<ConversationMessage> findByConversationIdOrderBySentAtAsc(UUID conversationId);

    @Query("SELECT m FROM ConversationMessage m WHERE m.conversation.id = :conversationId " +
           "AND m.status IN ('DELIVERED', 'SENT') AND m.direction = 'INBOUND' " +
           "ORDER BY m.sentAt DESC")
    List<ConversationMessage> findUnreadMessages(@Param("conversationId") UUID conversationId);

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.conversation.id = :conversationId " +
           "AND m.status IN ('DELIVERED', 'SENT') AND m.direction = 'INBOUND'")
    int countUnreadMessages(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("UPDATE ConversationMessage m SET m.status = 'READ', m.readAt = :readAt " +
           "WHERE m.conversation.id = :conversationId AND m.status IN ('DELIVERED', 'SENT') " +
           "AND m.direction = 'INBOUND'")
    int markConversationMessagesAsRead(@Param("conversationId") UUID conversationId, 
                                       @Param("readAt") LocalDateTime readAt);

    long countByConversationId(UUID conversationId);

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.conversation.tenantId = :tenantId " +
           "AND m.direction = :direction")
    long countByTenantAndDirection(@Param("tenantId") UUID tenantId, 
                                   @Param("direction") MessageDirection direction);

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.conversation.tenantId = :tenantId " +
           "AND m.fromBot = true")
    long countBotMessages(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(m) FROM ConversationMessage m WHERE m.conversation.tenantId = :tenantId " +
           "AND m.status = 'FAILED'")
    long countFailedMessages(@Param("tenantId") UUID tenantId);

    @Query("SELECT m FROM ConversationMessage m WHERE m.conversation.tenantId = :tenantId " +
           "AND m.status = 'PENDING' AND m.sentAt < :threshold")
    List<ConversationMessage> findStuckPendingMessages(@Param("tenantId") UUID tenantId, 
                                                        @Param("threshold") LocalDateTime threshold);

    @Query("SELECT m FROM ConversationMessage m WHERE m.conversation.id = :conversationId " +
           "AND m.sentAt >= :startDate AND m.sentAt <= :endDate " +
           "ORDER BY m.sentAt ASC")
    List<ConversationMessage> findMessagesByDateRange(@Param("conversationId") UUID conversationId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT m FROM ConversationMessage m WHERE m.conversation.tenantId = :tenantId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ConversationMessage> searchByContent(@Param("tenantId") UUID tenantId, 
                                              @Param("keyword") String keyword, 
                                              Pageable pageable);
}