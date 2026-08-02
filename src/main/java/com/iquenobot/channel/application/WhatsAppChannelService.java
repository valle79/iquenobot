package com.iquenobot.channel.application;

import com.iquenobot.ai.domain.service.IWhatsAppProvider;
import com.iquenobot.channel.domain.dto.CreateWhatsAppChannelRequestDto;
import com.iquenobot.channel.domain.dto.UpdateWhatsAppChannelRequestDto;
import com.iquenobot.channel.domain.dto.WhatsAppChannelDto;
import com.iquenobot.channel.domain.entity.WhatsAppChannel;
import com.iquenobot.channel.domain.repository.WhatsAppChannelRepository;
import com.iquenobot.channel.interfaces.mapper.WhatsAppChannelMapper;
import com.iquenobot.shared.domain.util.TenantContext;
import com.iquenobot.shared.enums.WhatsAppChannelStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppChannelService {

    private final WhatsAppChannelRepository channelRepository;
    private final WhatsAppChannelMapper channelMapper;
    private final IWhatsAppProvider whatsAppProvider;

    @Value("${app.base-url:http://localhost:8085}")
    private String backendBaseUrl;

    @Value("${EVOLUTION_WEBHOOK_BASE_URL:}")
    private String evolutionWebhookBaseUrl;

    @Data
    @Builder
    public static class ConnectionStatusResponse {
        private boolean connected;
        private String status;
        private String instanceName;
        private String phoneNumber;
        private String provider;
        private String error;
    }

    @Data
    @Builder
    public static class QRCodeResponse {
        private String base64;
        private boolean hasQR;
        private String instanceName;
        private String error;
    }

    @Data
    @Builder
    public static class TestConnectionResponse {
        private boolean success;
        private boolean connected;
        private String instanceName;
        private String message;
    }

    @Transactional(readOnly = true)
    public List<WhatsAppChannelDto> getAll() {
        UUID tenantId = getTenantId();
        return channelRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream().map(channelMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public WhatsAppChannelDto getById(UUID id) {
        return channelMapper.toDto(getChannel(id));
    }

    @Transactional
    public WhatsAppChannelDto create(CreateWhatsAppChannelRequestDto request) {
        UUID tenantId = getTenantId();
        String instanceName = request.getInstanceName().trim();
        String channelName = request.getChannelName().trim();

        if (channelRepository.existsByInstanceNameAndDeletedFalse(instanceName)) {
            throw new BusinessException("Ya existe un canal con el nombre de instancia: " + instanceName);
        }
        if (channelRepository.existsByTenantIdAndChannelNameAndDeletedFalse(tenantId, channelName)) {
            throw new BusinessException("Ya existe un canal con el nombre: " + channelName);
        }

        WhatsAppChannel channel = channelMapper.toEntity(request);
        channel.setId(UUID.randomUUID());
        channel.setTenantId(tenantId);
        channel.setInstanceName(instanceName);
        channel.setChannelName(channelName);
        channel.setStatus(WhatsAppChannelStatus.DISCONNECTED);

        channel = channelRepository.save(channel);

        configureWebhook(channel);

        log.info("WhatsApp channel created: id={} tenantId={} instance={}", channel.getId(), tenantId, instanceName);
        return channelMapper.toDto(channel);
    }

    @Transactional
    public WhatsAppChannelDto update(UUID id, UpdateWhatsAppChannelRequestDto request) {
        WhatsAppChannel channel = getChannel(id);

        if (request.getChannelName() != null && !request.getChannelName().isBlank()) {
            String channelName = request.getChannelName().trim();
            boolean nameTaken = channelRepository.findByTenantIdAndDeletedFalse(channel.getTenantId())
                    .stream().anyMatch(c -> !c.getId().equals(id) && c.getChannelName().equalsIgnoreCase(channelName));
            if (nameTaken) {
                throw new BusinessException("Ya existe un canal con el nombre: " + channelName);
            }
            channel.setChannelName(channelName);
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            channel.setPhoneNumber(request.getPhoneNumber().trim());
        }

        channel = channelRepository.save(channel);
        return channelMapper.toDto(channel);
    }

    @Transactional
    public void delete(UUID id) {
        WhatsAppChannel channel = getChannel(id);
        String userIdStr = TenantContext.getUserId();
        channel.softDelete(userIdStr != null ? UUID.fromString(userIdStr) : null);
        channelRepository.save(channel);
        log.info("WhatsApp channel soft-deleted: id={} instance={}", id, channel.getInstanceName());
    }

    @Transactional
    public TestConnectionResponse testConnection(UUID id) {
        WhatsAppChannel channel = getChannel(id);

        try {
            configureWebhook(channel);
            boolean connected = whatsAppProvider.isConnected(channel.getInstanceName());
            updateConnectionStatus(channel, connected);
            return TestConnectionResponse.builder()
                    .success(true)
                    .connected(connected)
                    .instanceName(channel.getInstanceName())
                    .message(connected
                            ? "Conexión exitosa con Evolution API"
                            : "La instancia no está conectada. Verifica que Evolution API esté corriendo y la instancia esté activa.")
                    .build();
        } catch (Exception e) {
            log.error("Error testing WhatsApp channel connection: id={} error={}", id, e.getMessage());
            updateStatus(channel, WhatsAppChannelStatus.ERROR);
            return TestConnectionResponse.builder()
                    .success(false)
                    .connected(false)
                    .instanceName(channel.getInstanceName())
                    .message("Error al conectar: " + e.getMessage())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public ConnectionStatusResponse getStatus(UUID id) {
        WhatsAppChannel channel = getChannel(id);
        String providerName = whatsAppProvider.getProviderType().name();

        boolean connected = false;
        String error = null;

        try {
            connected = whatsAppProvider.isConnected(channel.getInstanceName());
        } catch (Exception e) {
            log.warn("Error checking WhatsApp channel status: id={} error={}", id, e.getMessage());
            error = "No se pudo verificar el estado de conexión";
        }

        return ConnectionStatusResponse.builder()
                .connected(connected)
                .status(channel.getStatus() != null ? channel.getStatus().name() : null)
                .instanceName(channel.getInstanceName())
                .phoneNumber(channel.getPhoneNumber())
                .provider(providerName)
                .error(error)
                .build();
    }

    @Transactional
    public QRCodeResponse getQRCode(UUID id) {
        WhatsAppChannel channel = getChannel(id);

        try {
            configureWebhook(channel);
            updateStatus(channel, WhatsAppChannelStatus.CONNECTING);

            String qrBase64 = whatsAppProvider.getQRCode(channel.getInstanceName());
            if (qrBase64 != null) {
                String fullBase64 = qrBase64.startsWith("data:") ? qrBase64 : "data:image/png;base64," + qrBase64;
                return QRCodeResponse.builder()
                        .base64(fullBase64)
                        .hasQR(true)
                        .instanceName(channel.getInstanceName())
                        .build();
            }
            return QRCodeResponse.builder()
                    .hasQR(false)
                    .instanceName(channel.getInstanceName())
                    .error("No se pudo generar el código QR")
                    .build();
        } catch (Exception e) {
            log.warn("Error getting QR code for channel: id={} error={}", id, e.getMessage());
            updateStatus(channel, WhatsAppChannelStatus.ERROR);
            return QRCodeResponse.builder()
                    .hasQR(false)
                    .instanceName(channel.getInstanceName())
                    .error("Error al obtener código QR: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    public void disconnect(UUID id) {
        WhatsAppChannel channel = getChannel(id);
        try {
            whatsAppProvider.disconnect(channel.getInstanceName());
            updateStatus(channel, WhatsAppChannelStatus.DISCONNECTED);
            log.info("WhatsApp channel disconnected: id={} instance={}", id, channel.getInstanceName());
        } catch (Exception e) {
            log.error("Error disconnecting WhatsApp channel: id={} error={}", id, e.getMessage());
            throw new BusinessException("Error al desconectar WhatsApp: " + e.getMessage());
        }
    }

    private void updateConnectionStatus(WhatsAppChannel channel, boolean connected) {
        if (connected) {
            channel.setStatus(WhatsAppChannelStatus.CONNECTED);
            channel.setConnectedAt(LocalDateTime.now(ZoneOffset.UTC));
        } else {
            channel.setStatus(WhatsAppChannelStatus.DISCONNECTED);
            channel.setConnectedAt(null);
        }
        channelRepository.save(channel);
    }

    private void updateStatus(WhatsAppChannel channel, WhatsAppChannelStatus status) {
        channel.setStatus(status);
        if (status != WhatsAppChannelStatus.CONNECTED) {
            channel.setConnectedAt(null);
        }
        channelRepository.save(channel);
    }

    private void configureWebhook(WhatsAppChannel channel) {
        String webhookUrl = getWebhookUrl(channel.getInstanceName());
        whatsAppProvider.setWebhook(channel.getInstanceName(), webhookUrl);
        channel.setWebhookUrl(webhookUrl);
        channelRepository.save(channel);
    }

    private String getWebhookUrl(String instanceName) {
        String base = evolutionWebhookBaseUrl != null && !evolutionWebhookBaseUrl.isBlank()
                ? evolutionWebhookBaseUrl
                : backendBaseUrl;
        return base + "/api/v1/whatsapp/webhook/" + instanceName;
    }

    private WhatsAppChannel getChannel(UUID id) {
        return channelRepository.findByIdAndTenantIdAndDeletedFalse(id, getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Canal de WhatsApp no encontrado"));
    }

    private UUID getTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null) {
            throw new BusinessException("Contexto de tenant no disponible");
        }
        return UUID.fromString(tenantIdStr);
    }
}
