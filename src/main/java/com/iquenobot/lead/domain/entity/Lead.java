package com.iquenobot.lead.domain.entity;

import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.LeadSource;
import com.iquenobot.shared.enums.LeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leads")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Lead extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private LeadSource source;

    @Column(name = "source_details", length = 500)
    private String sourceDetails;

    @Column(name = "estimated_value", precision = 10, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "probability", precision = 5, scale = 2)
    private BigDecimal probability; // 0.00 to 100.00

    @Column(name = "score", nullable = false)
    private Integer score = 0; // Lead scoring: 0-100

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "first_contact_at")
    private LocalDateTime firstContactAt;

    @Column(name = "last_contact_at")
    private LocalDateTime lastContactAt;

    @Column(name = "expected_close_date")
    private LocalDateTime expectedCloseDate;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "lost_reason", length = 500)
    private String lostReason;

    @Column(name = "converted_to_contact_id")
    private String convertedToContactId;

    @Column(name = "tags", length = 500)
    private String tags; // JSON array of tags

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields; // JSON for custom fields

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Business methods
    public void assignToUser(User user) {
        this.assignedTo = user;
        this.assignedAt = LocalDateTime.now();
    }

    public void markAsContacted() {
        if (this.firstContactAt == null) {
            this.firstContactAt = LocalDateTime.now();
        }
        this.lastContactAt = LocalDateTime.now();
        
        if (this.status == LeadStatus.NEW) {
            this.status = LeadStatus.CONTACTED;
        }
    }

    public void markAsQualified(Integer score) {
        this.status = LeadStatus.QUALIFIED;
        this.score = score != null ? score : this.score;
    }

    public void markAsConverted(String contactId) {
        this.status = LeadStatus.CONVERTED;
        this.convertedToContactId = contactId;
        this.closedAt = LocalDateTime.now();
    }

    public void markAsLost(String reason) {
        this.status = LeadStatus.LOST;
        this.lostReason = reason;
        this.closedAt = LocalDateTime.now();
    }

    public void markAsDisqualified(String reason) {
        this.status = LeadStatus.DISQUALIFIED;
        this.lostReason = reason;
        this.closedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == LeadStatus.NEW || status == LeadStatus.CONTACTED || status == LeadStatus.QUALIFIED;
    }

    public boolean isClosed() {
        return status == LeadStatus.CONVERTED || status == LeadStatus.LOST || status == LeadStatus.DISQUALIFIED;
    }

    public Integer getDaysSinceCreated() {
        return (int) java.time.Duration.between(getCreatedAt(), LocalDateTime.now()).toDays();
    }

    public Integer getDaysSinceLastContact() {
        if (lastContactAt == null) {
            return null;
        }
        return (int) java.time.Duration.between(lastContactAt, LocalDateTime.now()).toDays();
    }
}
