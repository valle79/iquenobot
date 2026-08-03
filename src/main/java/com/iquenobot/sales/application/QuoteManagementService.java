package com.iquenobot.sales.application;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.auth.domain.entity.User;
import com.iquenobot.auth.domain.repository.TenantRepository;
import com.iquenobot.auth.domain.repository.UserRepository;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.orchestrator.domain.service.ChannelMessageSender;
import com.iquenobot.sales.domain.dto.QuoteDetailDto;
import com.iquenobot.sales.domain.dto.QuoteHistoryDto;
import com.iquenobot.sales.domain.dto.QuoteResendResultDto;
import com.iquenobot.sales.domain.dto.QuoteSummaryDto;
import com.iquenobot.sales.domain.entity.Quote;
import com.iquenobot.sales.domain.entity.QuoteHistory;
import com.iquenobot.sales.domain.model.QuoteItemDto;
import com.iquenobot.sales.domain.model.QuoteSearchCriteria;
import com.iquenobot.sales.domain.repository.QuoteHistoryRepository;
import com.iquenobot.sales.domain.repository.QuoteRepository;
import com.iquenobot.sales.domain.repository.QuoteSpecifications;
import com.iquenobot.sales.infrastructure.event.QuoteEventPublisher;
import com.iquenobot.sales.interfaces.event.QuoteEvent;
import com.iquenobot.sales.interfaces.event.QuoteEventType;
import com.iquenobot.shared.application.MessageTemplateResolver;
import com.iquenobot.shared.domain.dto.PagedResponse;
import com.iquenobot.shared.domain.service.StorageService;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.QuoteHistoryAction;
import com.iquenobot.shared.enums.QuoteStatus;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.exception.ResourceNotFoundException;
import com.iquenobot.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Management of historical quotes: listing with filters, detail, download,
 * resend (same or another WhatsApp number), regeneration, cancellation and
 * audit history. All operations are tenant-scoped.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteManagementService {

    private static final String BOT_ACTOR = "Bot";
    private static final String WHATSAPP_CHANNEL = ChannelType.WHATSAPP.name();

    private final QuoteRepository quoteRepository;
    private final QuoteHistoryRepository quoteHistoryRepository;
    private final QuoteHistoryService quoteHistoryService;
    private final QuoteService quoteService;
    private final StorageService storageService;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final com.iquenobot.setting.domain.repository.SettingRepository settingRepository;
    private final MessageTemplateResolver templateResolver;
    private final List<ChannelMessageSender> channelSenders;
    private final QuoteEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public PagedResponse<QuoteSummaryDto> search(QuoteSearchCriteria criteria, Pageable pageable) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant no identificado");
        }
        Specification<Quote> spec = QuoteSpecifications.withCriteria(criteria)
                .and((root, query, cb) -> cb.equal(root.get("tenantId"), tenantId));

        Page<Quote> page = quoteRepository.findAll(spec, pageable);
        Map<UUID, String> userNames = resolveUserNames(page.getContent().stream()
                .map(Quote::getCreatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<QuoteSummaryDto> content = page.getContent().stream()
                .map(q -> toSummary(q, userNames))
                .toList();

        return PagedResponse.<QuoteSummaryDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public QuoteDetailDto getDetail(UUID quoteId) {
        Quote quote = loadQuote(quoteId);
        List<QuoteItemDto> items = quoteService.getItems(quote);
        List<QuoteHistoryDto> history = quoteHistoryRepository
                .findByTenantIdAndQuoteIdOrderByCreatedAtAsc(quote.getTenantId(), quoteId).stream()
                .map(this::toHistoryDto)
                .toList();
        return toDetail(quote, items, history);
    }

    @Transactional(readOnly = true)
    public List<QuoteHistoryDto> getHistory(UUID quoteId) {
        Quote quote = loadQuote(quoteId);
        return quoteHistoryRepository
                .findByTenantIdAndQuoteIdOrderByCreatedAtAsc(quote.getTenantId(), quoteId).stream()
                .map(this::toHistoryDto)
                .toList();
    }

    /**
     * Downloads the stored PDF and verifies its integrity (SHA-256).
     */
    @Transactional
    public DownloadedPdf download(UUID quoteId) {
        Quote quote = loadQuote(quoteId);
        byte[] pdf = loadPdfBytes(quote);
        verifyIntegrity(quote, pdf);

        UUID userId = SecurityUtils.getCurrentUserId();
        String userName = resolveUserName(userId);
        quoteHistoryService.record(quote, QuoteHistoryAction.DOWNLOADED, userId, userName, null, null,
                "PDF descargado (" + formatSize(pdf.length) + ")");
        publish(quote, QuoteEventType.DOWNLOADED, userId, userName, null, null, null);

        String filename = quote.getFileName() != null ? quote.getFileName()
                : "cotizacion_" + quote.getQuoteNumber() + ".pdf";
        return new DownloadedPdf(pdf, filename);
    }

    /**
     * Resends the stored PDF to the quote's customer WhatsApp number without
     * regenerating the document.
     */
    @Transactional
    public QuoteResendResultDto resend(UUID quoteId) {
        Quote quote = loadQuote(quoteId);
        String phone = quote.getContact() != null ? quote.getContact().resolveWhatsAppNumber() : null;
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("El contacto de la cotización no tiene número de WhatsApp");
        }
        return doResend(quote, phone);
    }

    /**
     * Resends the stored PDF to a different WhatsApp number.
     */
    @Transactional
    public QuoteResendResultDto resendTo(UUID quoteId, String targetPhone) {
        if (targetPhone == null || targetPhone.isBlank()) {
            throw new BusinessException("Debe indicar el número de WhatsApp de destino");
        }
        Quote quote = loadQuote(quoteId);
        return doResend(quote, targetPhone.trim());
    }

    /**
     * Regenerates the PDF with the current tenant/contact data and replaces the
     * stored file (same storage key), updating size and hash.
     */
    @Transactional
    public QuoteDetailDto regenerate(UUID quoteId) {
        Quote quote = loadQuote(quoteId);
        Tenant tenant = loadTenant(quote);
        Contact contact = quote.getContact();

        byte[] pdf = quoteService.generatePdf(quote, tenant, contact);
        String key = quote.getStorageKey() != null
                ? quote.getStorageKey()
                : QuoteStorageKeyBuilder.build(quote.getQuoteNumber(), quote.getTenantId(), quote.getCreatedAt());
        StorageService.StoredObject stored = storageService.store(pdf, key, "application/pdf");
        String filename = "cotizacion_" + quote.getQuoteNumber() + ".pdf";

        quote.setPdfUrl(stored.publicUrl());
        quote.setStorageKey(stored.objectKey());
        quote.setFileName(filename);
        quote.setFileSize(stored.size());
        quote.setFileHash(stored.sha256Hash());
        quoteRepository.save(quote);

        UUID userId = SecurityUtils.getCurrentUserId();
        String userName = resolveUserName(userId);
        quoteHistoryService.record(quote, QuoteHistoryAction.REGENERATED, userId, userName, null, null,
                "PDF regenerado y reemplazado en el almacenamiento");
        publish(quote, QuoteEventType.REGENERATED, userId, userName, null, null, null);

        return getDetail(quoteId);
    }

    /**
     * Cancels the quote (soft delete + ANULADA).
     */
    @Transactional
    public void cancel(UUID quoteId) {
        Quote quote = loadQuote(quoteId);
        UUID userId = SecurityUtils.getCurrentUserId();
        String userName = resolveUserName(userId);

        quote.softDelete(userId);
        quote.setStatus(QuoteStatus.ANULADA);
        quoteRepository.save(quote);

        quoteHistoryService.record(quote, QuoteHistoryAction.CANCELLED, userId, userName, null, null,
                "Cotización anulada");
        publish(quote, QuoteEventType.CANCELLED, userId, userName, null, null, null);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private QuoteResendResultDto doResend(Quote quote, String targetPhone) {
        byte[] pdf = loadPdfBytes(quote);
        verifyIntegrity(quote, pdf);

        String mediaUrl = storageService.resolvePublicUrl(quote.getStorageKey());
        String filename = quote.getFileName() != null ? quote.getFileName()
                : "cotizacion_" + quote.getQuoteNumber() + ".pdf";
        String caption = buildCaption(quote);

        String instanceId = getWhatsAppInstanceId(quote.getTenantId());
        ChannelMessageSender sender = channelSenders.stream()
                .filter(s -> s.supportedChannel() == ChannelType.WHATSAPP)
                .findFirst()
                .orElse(null);
        if (instanceId == null || sender == null) {
            throw new BusinessException("No hay una instancia de WhatsApp configurada para reenviar la cotización");
        }

        UUID userId = SecurityUtils.getCurrentUserId();
        String userName = resolveUserName(userId);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        String messageId;
        try {
            messageId = sender.sendMediaMessage(instanceId, targetPhone, mediaUrl, caption, filename, MessageType.DOCUMENT);
        } catch (Exception e) {
            log.error("Failed to resend quote {} to {}: {}", quote.getQuoteNumber(), targetPhone, e.getMessage(), e);
            quoteHistoryService.record(quote, QuoteHistoryAction.ERROR, userId, userName, WHATSAPP_CHANNEL, null,
                    "Error al reenviar a " + targetPhone + ": " + e.getMessage());
            publish(quote, QuoteEventType.ERROR, userId, userName, WHATSAPP_CHANNEL, null,
                    "Error al reenviar a " + targetPhone);
            throw new BusinessException("No se pudo reenviar la cotización por WhatsApp: " + e.getMessage());
        }

        quote.setResendCount((quote.getResendCount() == null ? 0 : quote.getResendCount()) + 1);
        quote.setLastResentAt(now);
        quote.setStatus(QuoteStatus.REENVIADA);
        quoteRepository.save(quote);

        String details = targetPhone.equals(quote.getContact() != null ? quote.getContact().getPhone() : null)
                ? "Reenviada al cliente"
                : "Reenviada al número " + targetPhone;
        quoteHistoryService.record(quote, QuoteHistoryAction.RESENT, userId, userName, WHATSAPP_CHANNEL, messageId, details);
        publish(quote, QuoteEventType.RESENT, userId, userName, WHATSAPP_CHANNEL, messageId, details);

        log.info("Quote {} resent to {} (messageId={})", quote.getQuoteNumber(), targetPhone, messageId);
        return new QuoteResendResultDto(quote.getId(), quote.getQuoteNumber(), true,
                "Cotización reenviada exitosamente", WHATSAPP_CHANNEL, messageId, now);
    }

    private byte[] loadPdfBytes(Quote quote) {
        String key = quote.getStorageKey() != null
                ? quote.getStorageKey()
                : legacyKeyFromUrl(quote.getPdfUrl());
        if (key == null) {
            throw new BusinessException("La cotización no tiene un PDF almacenado");
        }
        if (!storageService.exists(key)) {
            throw new BusinessException("El archivo PDF de la cotización no existe en el almacenamiento");
        }
        return storageService.load(key);
    }

    private void verifyIntegrity(Quote quote, byte[] pdf) {
        if (quote.getFileHash() != null && !quote.getFileHash().isBlank()) {
            String current = com.iquenobot.shared.util.Sha256Util.hash(pdf);
            if (!quote.getFileHash().equalsIgnoreCase(current)) {
                throw new BusinessException("El archivo PDF no supera la verificación de integridad (hash no coincide)");
            }
        }
    }

    private String legacyKeyFromUrl(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return null;
        }
        int idx = pdfUrl.indexOf("/uploads/");
        return idx >= 0 ? pdfUrl.substring(idx + "/uploads/".length()) : null;
    }

    private String buildCaption(Quote quote) {
        String customerName = quote.getCustomerName() != null ? quote.getCustomerName() : "cliente";
        return "📄 Estimado(a) " + customerName + ", te reenvío la cotización "
                + quote.getQuoteNumber() + ". Para cualquier consulta, contáctanos. ¡Gracias por tu preferencia!";
    }

    private String getWhatsAppInstanceId(UUID tenantId) {
        return settingRepository
                .findByTenantIdAndCategoryAndKeyAndDeletedFalse(tenantId, "whatsapp", "instance_id")
                .map(setting -> setting.getValue())
                .orElse(null);
    }

    private Quote loadQuote(UUID quoteId) {
        UUID tenantId = SecurityUtils.getCurrentTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant no identificado");
        }
        return quoteRepository.findByIdAndTenantIdAndDeletedFalse(quoteId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada"));
    }

    private Tenant loadTenant(Quote quote) {
        return tenantRepository.findById(quote.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada"));
    }

    private Map<UUID, String> resolveUserNames(java.util.Set<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getFullName));
    }

    private String resolveUserName(UUID userId) {
        if (userId == null) {
            return BOT_ACTOR;
        }
        return userRepository.findById(userId).map(User::getFullName).orElse("Usuario");
    }

    private QuoteSummaryDto toSummary(Quote quote, Map<UUID, String> userNames) {
        Contact contact = quote.getContact();
        UUID generatedBy = quote.getCreatedBy();
        String generatedByName = generatedBy != null
                ? userNames.getOrDefault(generatedBy, "Usuario")
                : BOT_ACTOR;
        return new QuoteSummaryDto(
                quote.getId(),
                quote.getQuoteNumber(),
                contact != null ? contact.getFullName() : null,
                contact != null ? contact.getPhone() : null,
                contact != null ? contact.getWhatsappPhone() : null,
                quote.getCreatedAt(),
                quote.getTotal(),
                quote.getCurrency(),
                quote.getStatus(),
                quote.getResendCount() == null ? 0 : quote.getResendCount(),
                quote.getLastResentAt(),
                generatedBy,
                generatedByName,
                quote.getPdfUrl(),
                quote.getFileName(),
                quote.getFileSize());
    }

    private QuoteDetailDto toDetail(Quote quote, List<QuoteItemDto> items, List<QuoteHistoryDto> history) {
        UUID generatedBy = quote.getCreatedBy();
        String generatedByName = generatedBy != null ? resolveUserName(generatedBy) : BOT_ACTOR;
        Contact contact = quote.getContact();
        return new QuoteDetailDto(
                quote.getId(),
                quote.getQuoteNumber(),
                contact != null ? contact.getId() : null,
                contact != null ? contact.getFullName() : null,
                contact != null ? contact.getPhone() : null,
                contact != null ? contact.getWhatsappPhone() : null,
                quote.getConversation() != null ? quote.getConversation().getId() : null,
                quote.getCreatedAt(),
                quote.getCurrency(),
                quote.getSubtotal(),
                quote.getDiscount() != null ? quote.getDiscount() : java.math.BigDecimal.ZERO,
                quote.getIgv(),
                quote.getTotal(),
                quote.getStatus(),
                quote.getObservations(),
                quote.getFileName(),
                quote.getFileSize(),
                quote.getFileHash(),
                quote.getPdfUrl(),
                quote.getResendCount() == null ? 0 : quote.getResendCount(),
                quote.getLastResentAt(),
                generatedBy,
                generatedByName,
                items,
                history);
    }

    private QuoteHistoryDto toHistoryDto(QuoteHistory entry) {
        return new QuoteHistoryDto(
                entry.getId(),
                entry.getAction(),
                entry.getActorName(),
                entry.getChannel(),
                entry.getChannelMessageId(),
                entry.getDetails(),
                entry.getCreatedAt());
    }

    private void publish(Quote quote, QuoteEventType type, UUID actorId, String actorName,
                         String channel, String channelMessageId, String details) {
        eventPublisher.publish(new QuoteEvent(type, quote.getTenantId(), quote.getId(),
                quote.getQuoteNumber(), actorId, actorName, channel, channelMessageId, details));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public record DownloadedPdf(byte[] bytes, String filename) {}
}
