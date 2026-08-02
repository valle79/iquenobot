package com.iquenobot.channel.domain.entity;

import com.iquenobot.shared.common.SoftDeletableEntity;
import com.iquenobot.shared.enums.WhatsAppChannelStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_channels")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppChannel extends SoftDeletableEntity {

    @Column(name = "channel_name", nullable = false, length = 100)
    private String channelName;

    @Column(name = "instance_name", nullable = false, length = 120, unique = true)
    private String instanceName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WhatsAppChannelStatus status = WhatsAppChannelStatus.DISCONNECTED;

    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;
}
