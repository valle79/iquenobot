package com.iquenobot.conversation.domain.repository;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.ConversationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<Conversation> findByChannelConversationIdAndTenantIdAndDeletedFalse(
            String channelConversationId, UUID tenantId);

    Page<Conversation> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<Conversation> findByTenantIdAndStatusAndDeletedFalse(
            UUID tenantId, ConversationStatus status, Pageable pageable);

    Page<Conversation> findByTenantIdAndChannelAndDeletedFalse(
            UUID tenantId, ChannelType channel, Pageable pageable);

    Page<Conversation> findByTenantIdAndAssignedUserIdAndDeletedFalse(
            UUID tenantId, UUID assignedUserId, Pageable pageable);

    Page<Conversation> findByTenantIdAndContactIdAndDeletedFalse(
            UUID tenantId, UUID contactId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.assignedUser IS NULL " +
           "AND c.status IN ('OPEN', 'PENDING') AND c.deleted = false")
    Page<Conversation> findUnassignedConversations(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS', 'PENDING') AND c.deleted = false")
    Page<Conversation> findActiveConversations(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.priority = :priority " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS', 'PENDING') AND c.deleted = false")
    Page<Conversation> findByPriorityAndActive(@Param("tenantId") UUID tenantId, 
                                               @Param("priority") ConversationPriority priority, 
                                               Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.unreadCount > 0 " +
           "AND c.deleted = false")
    Page<Conversation> findWithUnreadMessages(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.lastMessageAt < :threshold AND c.status IN ('OPEN', 'IN_PROGRESS') " +
           "AND c.deleted = false")
    List<Conversation> findStaleConversations(@Param("tenantId") UUID tenantId, 
                                              @Param("threshold") LocalDateTime threshold);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ConversationStatus status);

    long countByTenantIdAndChannelAndDeletedFalse(UUID tenantId, ChannelType channel);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.assignedUser.id = :userId AND c.deleted = false")
    long countByAssignedUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.status IN ('OPEN', 'IN_PROGRESS', 'PENDING') AND c.deleted = false")
    long countActiveConversations(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.assignedUser IS NULL AND c.status IN ('OPEN', 'PENDING') AND c.deleted = false")
    long countUnassignedConversations(@Param("tenantId") UUID tenantId);

    @Query("SELECT AVG(c.responseTimeSeconds) FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.responseTimeSeconds IS NOT NULL AND c.deleted = false")
    Double getAverageResponseTime(@Param("tenantId") UUID tenantId);

    @Query("SELECT AVG(c.resolutionTimeSeconds) FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.resolutionTimeSeconds IS NOT NULL AND c.deleted = false")
    Double getAverageResolutionTime(@Param("tenantId") UUID tenantId);

    @Query("SELECT AVG(c.satisfactionRating) FROM Conversation c WHERE c.tenantId = :tenantId " +
           "AND c.satisfactionRating IS NOT NULL AND c.deleted = false")
    Double getAverageSatisfactionRating(@Param("tenantId") UUID tenantId);

    @Modifying
    @Query("""
            UPDATE Conversation c SET
                c.messageCount = c.messageCount + 1,
                c.unreadCount = c.unreadCount + 1,
                c.lastMessageAt = :lastMessageAt,
                c.updatedAt = :lastMessageAt
            WHERE c.id = :id AND c.tenantId = :tenantId
            """)
    int incrementIncomingMessageMetrics(@Param("id") UUID id,
                                        @Param("tenantId") UUID tenantId,
                                        @Param("lastMessageAt") java.time.LocalDateTime lastMessageAt);

    @Modifying
    @Query("""
            UPDATE Conversation c SET
                c.unreadCount = 0,
                c.updatedAt = :now
            WHERE c.id = :id AND c.tenantId = :tenantId
            """)
    int resetUnreadCount(@Param("id") UUID id,
                         @Param("tenantId") UUID tenantId,
                         @Param("now") java.time.LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE Conversation c SET
                c.messageCount = c.messageCount + 1,
                c.lastMessageAt = :lastMessageAt,
                c.updatedAt = :lastMessageAt
            WHERE c.id = :id AND c.tenantId = :tenantId
            """)
    int incrementOutgoingMessageMetrics(@Param("id") UUID id,
                                        @Param("tenantId") UUID tenantId,
                                        @Param("lastMessageAt") java.time.LocalDateTime lastMessageAt);

    @Modifying
    @Query("""
            UPDATE Conversation c SET
                c.firstResponseAt = :firstResponseAt,
                c.responseTimeSeconds = :responseTimeSeconds
            WHERE c.id = :id AND c.firstResponseAt IS NULL
            """)
    int recordFirstResponse(@Param("id") UUID id,
                            @Param("firstResponseAt") java.time.LocalDateTime firstResponseAt,
                            @Param("responseTimeSeconds") Long responseTimeSeconds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conversation c WHERE c.channelConversationId = :channelConversationId " +
           "AND c.tenantId = :tenantId AND c.deleted = false")
    Optional<Conversation> findByChannelConversationIdForUpdate(
            @Param("channelConversationId") String channelConversationId,
            @Param("tenantId") UUID tenantId);
}