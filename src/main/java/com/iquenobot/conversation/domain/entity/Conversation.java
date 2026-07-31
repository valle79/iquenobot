package com.iquenobot.conversation.domain.entity;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.ConversationStatus;
import com.iquenobot.shared.enums.ConversationPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Entity
@Table(name = "conversations")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private ChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ConversationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private ConversationPriority priority;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "channel_conversation_id", length = 100)
    private String channelConversationId;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "response_time_seconds")
    private Long responseTimeSeconds;

    @Column(name = "resolution_time_seconds")
    private Long resolutionTimeSeconds;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "unread_count", nullable = false)
    private Integer unreadCount = 0;

    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating;

    @Column(name = "satisfaction_feedback", length = 1000)
    private String satisfactionFeedback;

    @Column(name = "tags", length = 500)
    private String tags; // JSON array of tags

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON metadata

    @Column(name = "is_bot_conversation", nullable = false)
    private boolean botConversation = false;

    @Column(name = "bot_handoff_at")
    private LocalDateTime botHandoffAt;

    @Column(name = "bot_fallback_count", nullable = false)
    private int botFallbackCount = 0;

    @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
    private Set<ConversationMessage> messages;

    // Business methods
    public boolean isActive() {
        return !isDeleted() && status != ConversationStatus.CLOSED;
    }

    public boolean canBeAssigned() {
        return status == ConversationStatus.OPEN || status == ConversationStatus.PENDING;
    }

    public boolean isUnassigned() {
        return assignedUser == null;
    }

    public void assignTo(User user) {
        this.assignedUser = user;
        if (this.status == ConversationStatus.OPEN) {
            this.status = ConversationStatus.IN_PROGRESS;
        }
    }

    public void unassign() {
        this.assignedUser = null;
        if (this.status == ConversationStatus.IN_PROGRESS) {
            this.status = ConversationStatus.OPEN;
        }
    }

    public void markResolved() {
        this.status = ConversationStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now(ZoneOffset.UTC);
        if (this.responseTimeSeconds == null && this.firstResponseAt != null) {
            this.responseTimeSeconds = java.time.Duration.between(
                this.getCreatedAt(), this.firstResponseAt).toSeconds();
        }
        this.resolutionTimeSeconds = java.time.Duration.between(
            this.getCreatedAt(), this.resolvedAt).toSeconds();
    }

    public void markClosed() {
        this.status = ConversationStatus.CLOSED;
        this.closedAt = LocalDateTime.now(ZoneOffset.UTC);
        if (this.resolvedAt == null) {
            markResolved();
        }
    }

    public void reopen() {
        this.status = ConversationStatus.IN_PROGRESS;
        this.resolvedAt = null;
        this.closedAt = null;
        this.responseTimeSeconds = null;
        this.resolutionTimeSeconds = null;
    }

    public void incrementMessageCount() {
        this.messageCount++;
        this.lastMessageAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public void incrementUnreadCount() {
        this.unreadCount++;
    }

    public void resetUnreadCount() {
        this.unreadCount = 0;
    }

    public void recordFirstResponse() {
        if (this.firstResponseAt == null) {
            this.firstResponseAt = LocalDateTime.now(ZoneOffset.UTC);
            this.responseTimeSeconds = java.time.Duration.between(
                this.getCreatedAt(), this.firstResponseAt).toSeconds();
        }
    }

    public void handoffFromBot() {
        this.botConversation = false;
        this.botHandoffAt = LocalDateTime.now(ZoneOffset.UTC);
        this.status = ConversationStatus.OPEN;
    }

    public boolean isHighPriority() {
        return priority == ConversationPriority.HIGH || priority == ConversationPriority.URGENT;
    }

    public boolean needsAttention() {
        if (status == ConversationStatus.CLOSED || status == ConversationStatus.RESOLVED) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime threshold = now.minusHours(switch (priority) {
            case URGENT -> 1;
            case HIGH -> 4;
            case MEDIUM -> 8;
            case LOW -> 24;
        });
        
        return lastMessageAt.isBefore(threshold);
    }
}