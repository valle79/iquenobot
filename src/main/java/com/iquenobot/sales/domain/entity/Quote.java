package com.iquenobot.sales.domain.entity;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.QuoteStatus;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotes")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Quote extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "quote_number", nullable = false, length = 30)
    private String quoteNumber;

    @Column(name = "items", nullable = false, columnDefinition = "TEXT")
    private String items;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "currency", nullable = false, length = 5)
    private String currency = "PEN";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuoteStatus status;

    @Column(name = "pdf_url", length = 1000)
    private String pdfUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_hash", length = 128)
    private String fileHash;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "last_resent_at")
    private LocalDateTime lastResentAt;

    @Column(name = "resend_count", nullable = false)
    private Integer resendCount = 0;

    public String getCustomerName() {
        return contact != null ? contact.getFullName() : null;
    }
}
