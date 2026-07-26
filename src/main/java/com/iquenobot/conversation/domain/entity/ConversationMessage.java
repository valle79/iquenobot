package com.iquenobot.conversation.domain.entity;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.shared.common.BaseEntity;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.MessageStatus;
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
import java.util.Set;

@Entity
@Table(name = "conversation_messages")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private MessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private MessageType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MessageStatus status;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "channel_message_id", length = 100)
    private String channelMessageId;

    @Column(name = "reply_to_message_id")
    private String replyToMessageId;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "sender_phone", length = 20)
    private String senderPhone;

    @Column(name = "sender_email", length = 255)
    private String senderEmail;

    @Column(name = "is_from_bot", nullable = false)
    private boolean fromBot = false;

    @Column(name = "bot_intent", length = 100)
    private String botIntent;

    @Column(name = "bot_confidence")
    private Float botConfidence;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON metadata for channel-specific data

    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY)
    private Set<MessageAttachment> attachments;

    // Business methods
    public boolean isInbound() {
        return direction == MessageDirection.INBOUND;
    }

    public boolean isOutbound() {
        return direction == MessageDirection.OUTBOUND;
    }

    public boolean isDelivered() {
        return status == MessageStatus.DELIVERED || status == MessageStatus.READ;
    }

    public boolean isRead() {
        return status == MessageStatus.READ;
    }

    public boolean isFailed() {
        return status == MessageStatus.FAILED;
    }

    public void markDelivered() {
        if (this.status == MessageStatus.SENT) {
            this.status = MessageStatus.DELIVERED;
            this.deliveredAt = LocalDateTime.now();
        }
    }

    public void markRead() {
        if (this.status == MessageStatus.DELIVERED || this.status == MessageStatus.SENT) {
            this.status = MessageStatus.READ;
            this.readAt = LocalDateTime.now();
            if (this.deliveredAt == null) {
                this.deliveredAt = this.readAt;
            }
        }
    }

    public void markFailed(String reason) {
        this.status = MessageStatus.FAILED;
        this.failedAt = LocalDateTime.now();
        this.failureReason = reason;
    }

    public boolean hasAttachments() {
        return attachments != null && !attachments.isEmpty();
    }

    public boolean isFromCustomer() {
        return isInbound() && !fromBot;
    }

    public boolean isFromAgent() {
        return isOutbound() && !fromBot && user != null;
    }

    public boolean requiresResponse() {
        return isFromCustomer() && type == MessageType.TEXT;
    }

    public long getResponseTimeMinutes() {
        if (sentAt == null || conversation.getFirstResponseAt() == null) {
            return 0;
        }
        return java.time.Duration.between(sentAt, conversation.getFirstResponseAt()).toMinutes();
    }
}