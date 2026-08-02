package com.iquenobot.orchestrator.application.pipeline;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.channel.domain.entity.WhatsAppChannel;
import com.iquenobot.channel.domain.repository.WhatsAppChannelRepository;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.PipelineStep;
import com.iquenobot.setting.domain.entity.Setting;
import com.iquenobot.setting.domain.repository.SettingRepository;
import com.iquenobot.shared.domain.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantResolutionStep implements PipelineStep, MessagePipeline.PrioritizedStep {

    private static final String WHATSAPP_CATEGORY = "whatsapp";
    private static final String INSTANCE_ID_KEY = "instance_id";

    private final TenantRepository tenantRepository;
    private final SettingRepository settingRepository;
    private final WhatsAppChannelRepository whatsAppChannelRepository;

    @Override
    public int getOrder() { return 10; }

    @Override
    public ProcessingContext execute(ProcessingContext context) {
        var message = context.getIncomingMessage();
        final UUID tenantId = (message.getTenantId() != null)
            ? UUID.fromString(message.getTenantId())
            : (message.getInstanceId() != null)
            ? resolveTenantByInstance(message.getInstanceId())
            : null;

        if (tenantId == null) {
            throw new IllegalArgumentException(
                    "Cannot resolve tenant: provide tenantId or instanceId in message");
        }

        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        if (!tenant.isActive()) {
            throw new IllegalStateException("Tenant is not active: " + tenantId);
        }

        context.setTenantId(tenantId);
        context.setTenant(tenant);
        TenantContext.setTenantId(tenantId.toString());

        log.debug("Tenant resolved: id={} subdomain={}", tenantId, tenant.getSubdomain());
        return context;
    }

    /**
     * Resuelve el tenant del webhook por instance_name.
     * Primero busca en whatsapp_channels (nuevo esquema multi-número);
     * como fallback, usa el setting legacy whatsapp.instance_id.
     */
    private UUID resolveTenantByInstance(String instanceName) {
        Optional<UUID> channelTenant = whatsAppChannelRepository
                .findByInstanceNameAndDeletedFalse(instanceName)
                .map(WhatsAppChannel::getTenantId);

        if (channelTenant.isPresent()) {
            return channelTenant.get();
        }

        return settingRepository
                .findByCategoryAndKeyAndValueAndDeletedFalse(
                        WHATSAPP_CATEGORY, INSTANCE_ID_KEY, instanceName)
                .map(Setting::getTenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No tenant found for instance: " + instanceName));
    }
}
