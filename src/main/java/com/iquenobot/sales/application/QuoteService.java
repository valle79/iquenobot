package com.iquenobot.sales.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.conversation.domain.entity.Conversation;
import com.iquenobot.product.domain.entity.Product;
import com.iquenobot.sales.domain.entity.Quote;
import com.iquenobot.sales.domain.model.QuoteItemDto;
import com.iquenobot.sales.domain.repository.QuoteRepository;
import com.iquenobot.shared.enums.QuoteStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile(
            "(\\d+)\\s*(unidades?|unds?|unid\\.?|und\\.?)");

    /**
     * Formas de pago que el cliente puede mencionar al solicitar la cotización.
     * Se capturan en las observaciones de la cotización (y por tanto en el PDF).
     */
    private static final Pattern PAYMENT_PATTERN = Pattern.compile(
            "\\b(al\\s+|de\\s+)?(contado|cash|crédito|credito|letras|plazos|financiado"
                    + "|transferencia|yape|plin|efectivo|contra\\s+entrega|depósito|deposito)\\b",
            Pattern.CASE_INSENSITIVE);

    private final QuoteRepository quoteRepository;
    private final QuotePdfGenerator pdfGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Genera y persiste una cotización con los productos solicitados.
     * El número de cotización es secuencial (C-0000001, C-0000002, ...).
     */
    @Transactional
    public Quote createQuote(Tenant tenant, Contact contact, Conversation conversation,
                             List<Product> products, String customerMessage) {
        List<QuoteItemDto> items = buildItems(products, customerMessage);

        BigDecimal subtotal = items.stream()
                .map(QuoteItemDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv);

        String quoteNumber = nextQuoteNumber();

        Quote quote = Quote.builder()
                .tenantId(tenant.getId())
                .contact(contact)
                .conversation(conversation)
                .quoteNumber(quoteNumber)
                .items(toJson(items))
                .subtotal(subtotal)
                .igv(igv)
                .total(total)
                .currency("PEN")
                .status(QuoteStatus.SENT)
                .observations(extractPaymentObservation(customerMessage))
                .build();

        Quote saved = quoteRepository.save(quote);
        log.info("Quote {} created for contact {} in tenant {}", quoteNumber, contact.getId(), tenant.getId());
        return saved;
    }

    /**
     * Genera el PDF de una cotización con los datos actuales del tenant y contacto.
     */
    public byte[] generatePdf(Quote quote, Tenant tenant, Contact contact) {
        List<QuoteItemDto> items = fromJson(quote.getItems());
        return pdfGenerator.generate(
                quote.getQuoteNumber(),
                tenant,
                contact,
                items,
                quote.getSubtotal(),
                quote.getDiscount() != null ? quote.getDiscount() : BigDecimal.ZERO,
                quote.getIgv(),
                quote.getTotal(),
                quote.getObservations());
    }

    @Transactional
    public Quote save(Quote quote) {
        return quoteRepository.save(quote);
    }

    /**
     * Deserializa los ítems almacenados de una cotización.
     */
    public List<QuoteItemDto> getItems(Quote quote) {
        return fromJson(quote.getItems());
    }

    private List<QuoteItemDto> buildItems(List<Product> products, String customerMessage) {
        Integer mentionedQuantity = extractQuantity(customerMessage);

        return products.stream()
                .map(p -> new QuoteItemDto(
                        p.getName(),
                        p.getSku(),
                        mentionedQuantity != null ? mentionedQuantity : 1,
                        p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO))
                .toList();
    }

    private Integer extractQuantity(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = QUANTITY_PATTERN.matcher(message.toLowerCase());
        if (matcher.find()) {
            try {
                int quantity = Integer.parseInt(matcher.group(1));
                return quantity >= 1 && quantity <= 100 ? quantity : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extrae la forma de pago mencionada por el cliente en su mensaje para
     * incluirla en las observaciones de la cotización. Devuelve null si el
     * cliente no mencionó ninguna.
     */
    private String extractPaymentObservation(String customerMessage) {
        if (customerMessage == null || customerMessage.isBlank()) {
            return null;
        }
        Matcher matcher = PAYMENT_PATTERN.matcher(customerMessage.toLowerCase());
        StringBuilder payment = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group().trim().replaceAll("\\s+", " ");
            if (payment.indexOf(match) < 0) {
                if (payment.length() > 0) {
                    payment.append(", ");
                }
                payment.append(match);
            }
        }
        return payment.length() == 0 ? null : "Forma de pago: " + payment;
    }

    private String nextQuoteNumber() {
        Number next = (Number) entityManager.createNativeQuery("SELECT nextval('quote_number_seq')")
                .getSingleResult();
        return String.format("C-%07d", next.longValue());
    }

    private String toJson(List<QuoteItemDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error serializando ítems de cotización", e);
        }
    }

    private List<QuoteItemDto> fromJson(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuoteItemDto.class));
        } catch (JsonProcessingException e) {
            log.warn("Error deserializando ítems de cotización {}: {}", json, e.getMessage());
            return List.of();
        }
    }
}
