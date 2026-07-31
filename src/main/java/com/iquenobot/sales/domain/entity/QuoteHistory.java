package com.iquenobot.sales.domain.entity;

import com.iquenobot.shared.common.BaseEntity;
import com.iquenobot.shared.enums.QuoteHistoryAction;
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

import java.util.UUID;

/**
 * Audit trail of a quote: generation, initial send, resends, errors,
 * regenerations, downloads and cancellations.
 */
@Entity
@Table(name = "quote_history")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private QuoteHistoryAction action;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Column(name = "channel", length = 20)
    private String channel;

    @Column(name = "channel_message_id", length = 100)
    private String channelMessageId;

    @Column(name = "details", length = 1000)
    private String details;
}
