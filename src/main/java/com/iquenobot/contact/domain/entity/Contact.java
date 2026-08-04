package com.iquenobot.contact.domain.entity;

import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.ContactStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "contacts")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Contact extends SoftDeletableEntity {

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "whatsapp_phone", length = 50)
    private String whatsappPhone;

    @Column(name = "normalized_phone", length = 20)
    private String normalizedPhone;

    @Column(name = "company", length = 200)
    private String company;

    @Column(name = "document_type", length = 20)
    private String documentType;

    @Column(name = "document_number", length = 11)
    private String documentNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ContactStatus status;

    @Column(name = "language", length = 5)
    private String language;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "tags", length = 500)
    private String tags; // JSON array of tags

    @Column(name = "custom_fields", columnDefinition = "TEXT")
    private String customFields; // JSON for custom fields

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "last_contacted_at")
    private LocalDateTime lastContactedAt;

    @Column(name = "conversation_count", nullable = false)
    private Integer conversationCount = 0;

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @Column(name = "is_subscribed", nullable = false)
    private boolean subscribed = true;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "blocked_reason", length = 500)
    private String blockedReason;

    @OneToMany(mappedBy = "contact", fetch = FetchType.LAZY)
    private Set<Conversation> conversations;

    // Business methods
    public boolean isActive() {
        return !isDeleted() && status == ContactStatus.ACTIVE;
    }

    public boolean isBlocked() {
        return status == ContactStatus.BLOCKED || blockedAt != null;
    }

    public boolean canReceiveMessages() {
        return isActive() && subscribed && !isBlocked();
    }

    public void block(String reason) {
        this.status = ContactStatus.BLOCKED;
        this.blockedAt = LocalDateTime.now();
        this.blockedReason = reason;
    }

    public void unblock() {
        this.status = ContactStatus.ACTIVE;
        this.blockedAt = null;
        this.blockedReason = null;
    }

    public void unsubscribe() {
        this.subscribed = false;
        this.unsubscribedAt = LocalDateTime.now();
    }

    public void resubscribe() {
        this.subscribed = true;
        this.unsubscribedAt = null;
    }

    public void incrementConversationCount() {
        this.conversationCount++;
        this.lastContactedAt = LocalDateTime.now();
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }

    public void updateFullName() {
        if (firstName != null && lastName != null) {
            this.fullName = firstName + " " + lastName;
        } else if (firstName != null) {
            this.fullName = firstName;
        } else if (lastName != null) {
            this.fullName = lastName;
        } else if (phone != null) {
            this.fullName = phone;
        } else if (email != null) {
            this.fullName = email;
        }
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        if (firstName != null && !firstName.isBlank()) {
            return firstName;
        }
        if (phone != null) {
            return phone;
        }
        if (email != null) {
            return email;
        }
        return "Unknown Contact";
    }

    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    public boolean hasPhone() {
        return phone != null && !phone.isBlank();
    }

    public boolean hasWhatsApp() {
        return whatsappPhone != null && !whatsappPhone.isBlank();
    }

    /**
     * Número de destino para envíos por WhatsApp (formato internacional sin "+", ej: 51960947459).
     * Prioriza el número normalizado (con código de país), luego el número de WhatsApp,
     * y al final el número crudo.
     */
    public String resolveWhatsAppNumber() {

        // WhatsApp con privacidad (LID): el número está oculto y el
        // JID "user@lid" es la única vía para enviarle; se usa tal cual.
        if (hasText(whatsappPhone)) {
            String wa = whatsappPhone.trim();
            if (wa.endsWith("@lid")) {
                return wa;
            }
        }

        String candidate;
        if (hasText(normalizedPhone)) {
            candidate = normalizedPhone;
        } else if (hasText(whatsappPhone)) {
            candidate = whatsappPhone;
        } else {
            candidate = phone;
        }
        return candidate != null ? candidate.replace("+", "") : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}