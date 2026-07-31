package com.iquenobot.orchestrator.application.action;

import com.iquenobot.conversation.domain.entity.ConversationMessage;
import com.iquenobot.conversation.domain.entity.MessageAttachment;
import com.iquenobot.conversation.domain.repository.ConversationMessageRepository;
import com.iquenobot.conversation.domain.repository.ConversationRepository;
import com.iquenobot.conversation.domain.repository.MessageAttachmentRepository;
import com.iquenobot.orchestrator.domain.model.ActionType;
import com.iquenobot.orchestrator.domain.model.Decision;
import com.iquenobot.orchestrator.domain.model.ProcessingContext;
import com.iquenobot.orchestrator.domain.service.ActionExecutor;
import com.iquenobot.orchestrator.domain.service.ChannelMessageSender;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.product.domain.repository.ProductRepository;
import com.iquenobot.sales.application.QuoteHistoryService;
import com.iquenobot.sales.application.QuoteService;
import com.iquenobot.sales.application.QuoteStorageKeyBuilder;
import com.iquenobot.sales.domain.entity.Quote;
import com.iquenobot.sales.infrastructure.event.QuoteEventPublisher;
import com.iquenobot.sales.interfaces.event.QuoteEvent;
import com.iquenobot.sales.interfaces.event.QuoteEventType;
import com.iquenobot.shared.application.MessageTemplateResolver;
import com.iquenobot.shared.domain.service.StorageService;
import com.iquenobot.shared.enums.AttachmentType;
import com.iquenobot.shared.enums.ChannelType;
import com.iquenobot.shared.enums.MessageDirection;
import com.iquenobot.shared.enums.MessageStatus;
import com.iquenobot.shared.enums.MessageType;
import com.iquenobot.shared.enums.QuoteHistoryAction;
import com.iquenobot.shared.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Genera el PDF de cotización con los productos solicitados por el cliente
 * y lo envía por el canal (WhatsApp) como documento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendQuoteExecutor implements ActionExecutor {

    private final QuoteService quoteService;
    private final ProductRepository productRepository;
    private final StorageService storageService;
    private final MessageTemplateResolver templateResolver;
    private final ConversationMessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageAttachmentRepository attachmentRepository;
    private final QuoteHistoryService quoteHistoryService;
    private final QuoteEventPublisher eventPublisher;
    private final List<ChannelMessageSender> channelSenders;

    @Override
    public ActionType supportedActionType() { return ActionType.SEND_QUOTE; }

    @Override
    @Transactional
    public void execute(Decision decision, ProcessingContext context) {
        var params = decision.getParameters();
        if (params == null) {
            log.warn("SendQuoteExecutor called but no parameters provided");
            return;
        }

        List<Product> products = resolveProducts(params);
        if (products.isEmpty()) {
            log.warn("SendQuoteExecutor called but no products to quote for conversation {}",
                    context.getConversation().getId());
            return;
        }

        String customerMessage = (String) params.get("messageContent");

        Quote quote = null;
        try {
            quote = quoteService.createQuote(
                    context.getTenant(), context.getContact(), context.getConversation(),
                    products, customerMessage);

            byte[] pdf = quoteService.generatePdf(quote, context.getTenant(), context.getContact());
            String filename = "cotizacion_" + quote.getQuoteNumber() + ".pdf";
            String storageKey = QuoteStorageKeyBuilder.build(
                    quote.getQuoteNumber(), quote.getTenantId(), LocalDateTime.now(ZoneOffset.UTC));
            StorageService.StoredObject stored = storageService.store(pdf, storageKey, "application/pdf");

            quote.setPdfUrl(stored.publicUrl());
            quote.setStorageKey(stored.objectKey());
            quote.setFileName(filename);
            quote.setFileSize(stored.size());
            quote.setFileHash(stored.sha256Hash());
            quoteService.save(quote);

            quoteHistoryService.record(quote, QuoteHistoryAction.GENERATED, null, null, null, null,
                    "Cotización generada por el bot con " + products.size() + " producto(s)");

            String caption = buildCaption(context, quote);
            ChannelType channel = context.getIncomingMessage().getChannel();
            ChannelMessageSender sender = channelSenders.stream()
                    .filter(s -> s.supportedChannel() == channel)
                    .findFirst()
                    .orElse(null);

            if (sender == null) {
                log.warn("No channel sender available for channel={}", channel);
                return;
            }

            String recipient = switch (channel) {
                case WHATSAPP, SMS -> context.getContact().getPhone();
                default -> context.getIncomingMessage().getSourceIdentifier();
            };

            String messageId = sender.sendMediaMessage(
                    context.getIncomingMessage().getInstanceId(), recipient,
                    quote.getPdfUrl(), caption, filename, MessageType.DOCUMENT);

            quoteHistoryService.record(quote, QuoteHistoryAction.SENT, null, null,
                    channel.name(), messageId, "Envío inicial al cliente");
            eventPublisher.publish(new QuoteEvent(QuoteEventType.SENT, quote.getTenantId(), quote.getId(),
                    quote.getQuoteNumber(), null, "Bot", channel.name(), messageId, "Envío inicial al cliente"));

            ConversationMessage botMessage = ConversationMessage.builder()
                    .id(UUID.randomUUID())
                    .tenantId(context.getTenantId())
                    .conversation(context.getConversation())
                    .direction(MessageDirection.OUTBOUND)
                    .senderType(SenderType.BOT)
                    .type(MessageType.DOCUMENT)
                    .status(MessageStatus.SENT)
                    .content(caption != null ? caption : "")
                    .channelMessageId(messageId)
                    .fromBot(true)
                    .botIntent("quote")
                    .sentAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build();

            botMessage = messageRepository.save(botMessage);

            MessageAttachment attachment = MessageAttachment.builder()
                    .id(UUID.randomUUID())
                    .tenantId(context.getTenantId())
                    .message(botMessage)
                    .type(AttachmentType.DOCUMENT)
                    .fileName(filename)
                    .fileUrl(quote.getPdfUrl())
                    .caption(caption)
                    .build();
            attachmentRepository.save(attachment);

            var conv = context.getConversation();
            conv.incrementMessageCount();
            conversationRepository.save(conv);

            log.info("Quote {} sent to {} via {} (messageId={})",
                    quote.getQuoteNumber(), recipient, channel, messageId);

        } catch (Exception e) {
            log.error("Failed to generate/send quote for conversation {}: {}",
                    context.getConversation().getId(), e.getMessage(), e);
            if (quote != null) {
                try {
                    quoteHistoryService.record(quote, QuoteHistoryAction.ERROR, null, null, null, null,
                            "Error en generación/envío: " + e.getMessage());
                    eventPublisher.publish(new QuoteEvent(QuoteEventType.ERROR, quote.getTenantId(),
                            quote.getId(), quote.getQuoteNumber(), null, "Bot", null, null,
                            "Error en generación/envío: " + e.getMessage()));
                } catch (Exception ignored) {
                    log.warn("Could not record quote error history for quote {}", quote.getQuoteNumber());
                }
            }
        }
    }

    private List<Product> resolveProducts(Map<String, Object> params) {
        Object raw = params.get("productIds");
        if (raw == null) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        for (String id : raw.toString().split(",")) {
            try {
                ids.add(UUID.fromString(id.trim()));
            } catch (IllegalArgumentException ignored) {
                // ignore malformed id
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return productRepository.findAllById(ids);
    }

    private String buildCaption(ProcessingContext context, Quote quote) {
        String template = "📄 Estimado(a) {{customer_name}}, te adjunto la cotización "
                + quote.getQuoteNumber()
                + " con los productos que solicitaste. Para cualquier consulta, "
                + "contáctanos al {{contact_phone}} o al {{contact_email}}. ¡Gracias por tu preferencia!";
        return templateResolver.resolve(template, context.getTenant(), context.getContact());
    }
}
