package com.iquenobot.channel.domain.repository;

import com.iquenobot.channel.domain.entity.WhatsAppChannel;
import com.iquenobot.shared.enums.WhatsAppChannelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppChannelRepository extends JpaRepository<WhatsAppChannel, UUID> {

    Optional<WhatsAppChannel> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<WhatsAppChannel> findByInstanceNameAndDeletedFalse(String instanceName);

    Optional<WhatsAppChannel> findByTenantIdAndInstanceNameAndDeletedFalse(UUID tenantId, String instanceName);

    List<WhatsAppChannel> findByTenantIdAndDeletedFalse(UUID tenantId);

    List<WhatsAppChannel> findByTenantIdAndStatusAndDeletedFalse(UUID tenantId, WhatsAppChannelStatus status);

    boolean existsByTenantIdAndChannelNameAndDeletedFalse(UUID tenantId, String channelName);

    boolean existsByInstanceNameAndDeletedFalse(String instanceName);

    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
