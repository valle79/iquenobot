package com.iquenobot.shared.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    public void softDelete(UUID userId) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.deletedBy = userId;
    }

    public boolean isActive() {
        return !this.deleted;
    }
}