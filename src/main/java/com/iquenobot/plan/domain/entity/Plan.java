package com.iquenobot.plan.domain.entity;

import com.iquenobot.shared.common.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "plans")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Plan extends SoftDeletableEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(name = "yearly_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_conversations")
    private Integer maxConversations;

    @Column(name = "max_contacts")
    private Integer maxContacts;

    @Column(name = "max_storage_mb")
    private Integer maxStorageMb;

    @Column(name = "features", columnDefinition = "TEXT")
    private String features;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "is_public", nullable = false)
    private boolean publicPlan;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
