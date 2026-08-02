package com.iquenobot.sales.application;

import com.iquenobot.auth.domain.entity.Tenant;
import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.sales.domain.model.QuoteItemDto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Genera el PDF de cotización replicando el formato oficial de la empresa:
 * encabezado con datos del negocio, datos del cliente, tabla de ítems,
 * subtotal, IGV y total, y pie de página.
 */
@Component
@Slf4j
public class QuotePdfGenerator {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font BODY_BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FOOTER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8);

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);
    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    public byte[] generate(String quoteNumber, Tenant tenant, Contact contact,
                           List<QuoteItemDto> items, BigDecimal subtotal, BigDecimal igv, BigDecimal total) {
        return generate(quoteNumber, tenant, contact, items, subtotal, BigDecimal.ZERO, igv, total, null);
    }

    public byte[] generate(String quoteNumber, Tenant tenant, Contact contact,
                           List<QuoteItemDto> items, BigDecimal subtotal, BigDecimal discount,
                           BigDecimal igv, BigDecimal total, String observations) {
        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            writeHeader(document, quoteNumber, tenant);
            writeCustomerSection(document, quoteNumber, tenant, contact);
            writeItemsTable(document, items);
            writeTotals(document, subtotal, discount, igv, total);
            writeObservations(document, observations);
            writeFooter(document, tenant);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Error generando el PDF de cotización: " + e.getMessage(), e);
        }
    }

    private void writeHeader(Document document, String quoteNumber, Tenant tenant) {
        String businessName = firstNonBlank(tenant.getBusinessName(), tenant.getCompanyName(), "Mi Empresa");
        Paragraph title = new Paragraph(businessName, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        StringBuilder header = new StringBuilder();
        if (hasText(tenant.getRuc())) {
            header.append("RUC: ").append(tenant.getRuc());
        }
        if (hasText(tenant.getAddress())) {
            if (header.length() > 0) header.append(" | ");
            header.append(tenant.getAddress());
        }
        if (hasText(tenant.getCity())) {
            header.append(hasText(tenant.getCountry()) ? ", " : " | ");
            header.append(tenant.getCity());
            if (hasText(tenant.getCountry())) {
                header.append(", ").append(tenant.getCountry());
            }
        }
        if (hasText(tenant.getContactPhone())) {
            if (header.length() > 0) header.append(" | ");
            header.append("Tel: ").append(tenant.getContactPhone());
        }
        if (hasText(tenant.getContactEmail())) {
            if (header.length() > 0) header.append(" | ");
            header.append("Email: ").append(tenant.getContactEmail());
        }

        Paragraph subtitle = new Paragraph(header.toString(), SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);

        Paragraph spacer = new Paragraph(" ", SUBTITLE_FONT);
        spacer.setLeading(8);
        document.add(spacer);

        Paragraph quoteTitle = new Paragraph("Cotización N° " + quoteNumber, HEADER_FONT);
        quoteTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(quoteTitle);

        Paragraph date = new Paragraph("Fecha Emisión: " + LocalDate.now().format(DATE_FORMAT), BODY_FONT);
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);

        document.add(Chunk.NEWLINE);
    }

    private void writeCustomerSection(Document document, String quoteNumber, Tenant tenant, Contact contact) {
        PdfPTable table = new PdfPTable(new float[]{3, 7});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        addCell(table, "Campo", true);
        addCell(table, "Detalles", true);
        addCell(table, "Cliente", false);
        addCell(table, customerName(contact), false);

        if (hasText(contact.getDocumentType()) && hasText(contact.getDocumentNumber())) {
            addCell(table, contact.getDocumentType(), false);
            addCell(table, contact.getDocumentNumber(), false);
        }
        if (hasText(contact.getCompany()) && !contact.getCompany().equals(customerName(contact))) {
            addCell(table, "Razón Social", false);
            addCell(table, contact.getCompany(), false);
        }
        if (hasText(contact.getAddress())) {
            addCell(table, "Dirección", false);
            addCell(table, contact.getAddress(), false);
        }

        if (hasText(contact.getPhone())) {
            addCell(table, "Teléfono", false);
            addCell(table, contact.getPhone(), false);
        }
        if (hasText(contact.getEmail())) {
            addCell(table, "Email", false);
            addCell(table, contact.getEmail(), false);
        }

        addCell(table, "Asesor", false);
        addCell(table, "Equipo de ventas", false);

        document.add(table);
    }

    private void writeItemsTable(Document document, List<QuoteItemDto> items) {
        PdfPTable table = new PdfPTable(new float[]{4.5f, 1.2f, 2f, 2.3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        addCell(table, "Item", true);
        addCell(table, "Cantidad", true);
        addCell(table, "Precio Unitario", true);
        addCell(table, "Subtotal", true);

        if (items.isEmpty()) {
            addCell(table, "Sin ítems", false);
            addCell(table, "-", false);
            addCell(table, "-", false);
            addCell(table, "-", false);
        } else {
            for (QuoteItemDto item : items) {
                addCell(table, item.name(), false);
                addCell(table, String.valueOf(item.quantity()), false);
                addCell(table, money(item.unitPrice()), false);
                addCell(table, money(item.getSubtotal()), false);
            }
        }

        document.add(table);
    }

    private void writeTotals(Document document, BigDecimal subtotal, BigDecimal discount,
                             BigDecimal igv, BigDecimal total) {
        PdfPTable table = new PdfPTable(new float[]{6, 4});
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        addCell(table, "Subtotal:", false);
        addCell(table, money(subtotal), false);
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            addCell(table, "Descuento:", false);
            addCell(table, "- " + money(discount), false);
        }
        addCell(table, "IGV (18%):", false);
        addCell(table, money(igv), false);
        addCell(table, "Total:", true);
        addCell(table, money(total), true);

        document.add(table);
    }

    private void writeObservations(Document document, String observations) {
        if (observations == null || observations.isBlank()) {
            return;
        }
        Paragraph title = new Paragraph("Observaciones", BODY_BOLD_FONT);
        title.setSpacingBefore(6);
        document.add(title);
        Paragraph text = new Paragraph(observations, BODY_FONT);
        document.add(text);
    }

    private void writeFooter(Document document, Tenant tenant) {
        String businessName = firstNonBlank(tenant.getBusinessName(), tenant.getCompanyName(), "Mi Empresa");

        Paragraph thanks = new Paragraph("Gracias por su preferencia, te esperamos pronto.", BODY_FONT);
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingBefore(12);
        document.add(thanks);

        StringBuilder footer = new StringBuilder();
        footer.append(businessName).append(" | Calidad y confianza en cada proyecto");
        if (hasText(tenant.getRuc())) {
            footer.append("\nRUC: ").append(tenant.getRuc());
        }
        if (hasText(tenant.getAddress())) {
            footer.append(" | ").append(tenant.getAddress());
        }
        if (hasText(tenant.getContactPhone())) {
            footer.append(" | Tel: ").append(tenant.getContactPhone());
        }
        if (hasText(tenant.getContactEmail())) {
            footer.append(" | Email: ").append(tenant.getContactEmail());
        }

        Paragraph footerP = new Paragraph(footer.toString(), FOOTER_FONT);
        footerP.setAlignment(Element.ALIGN_CENTER);
        document.add(footerP);
    }

    private void addCell(PdfPTable table, String text, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", header ? HEADER_FONT : BODY_FONT));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5);
        if (header) {
            cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
        }
        table.addCell(cell);
    }

    private String customerName(Contact contact) {
        if (contact == null) {
            return "Cliente";
        }
        if (hasText(contact.getFullName())) {
            return contact.getFullName();
        }
        String first = contact.getFirstName() != null ? contact.getFirstName() : "";
        String last = contact.getLastName() != null ? contact.getLastName() : "";
        return (first + " " + last).trim().isEmpty() ? "Cliente" : (first + " " + last).trim();
    }

    private String money(BigDecimal value) {
        return value != null ? "S/ " + MONEY.format(value) : "S/ 0.00";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
