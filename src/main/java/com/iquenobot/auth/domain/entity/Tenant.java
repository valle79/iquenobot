package com.iquenobot.auth.domain.entity;

import com.iquenobot.plan.domain.entity.Plan;
import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.TenantStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends SoftDeletableEntity {

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "subdomain", nullable = false, unique = true, length = 50)
    private String subdomain;

    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "currency", length = 5)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status;

    @Column(name = "subscription_plan", length = 50)
    private String subscriptionPlan;

    @Column(name = "subscription_expires_at")
    private LocalDate subscriptionExpiresAt;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_conversations")
    private Integer maxConversations;

    @Column(name = "features", length = 2000)
    private String features;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "ruc", length = 20)
    private String ruc;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "locale", length = 10)
    private String locale;

    @OneToMany(mappedBy = "tenant")
    private Set<User> users;

    // Business methods
    public boolean isActive() {
        return !isDeleted() && status == TenantStatus.ACTIVE;
    }

    public boolean isSubscriptionValid() {
        return subscriptionExpiresAt != null && 
               subscriptionExpiresAt.isAfter(LocalDate.now());
    }

    public boolean canCreateMoreUsers(int currentUserCount) {
        return maxUsers == null || currentUserCount < maxUsers;
    }

    public boolean canCreateMoreConversations(int currentConversationCount) {
        return maxConversations == null || currentConversationCount < maxConversations;
    }
}