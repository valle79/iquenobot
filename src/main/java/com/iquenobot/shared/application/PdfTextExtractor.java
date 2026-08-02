package com.iquenobot.shared.application;

import com.iquenobot.shared.exception.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/**
 * Utilidad para extraer texto de documentos PDF usando Apache PDFBox.
 * Es una función pura: no mantiene estado y no depende de Spring.
 */
public final class PdfTextExtractor {

    public static final int MAX_PDF_PAGES = 200;
    private static final int MAX_EXTRACTED_CHARS = 1_000_000;

    private PdfTextExtractor() {
    }

    public static PdfExtractionResult extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException("El archivo PDF está vacío");
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount > MAX_PDF_PAGES) {
                throw new BusinessException(
                        "El PDF tiene " + pageCount + " páginas y el máximo permitido es " + MAX_PDF_PAGES
                                + ". Revisa que hayas seleccionado el archivo correcto.");
            }

            String text = new PDFTextStripper().getText(document);
            if (text != null && text.length() > MAX_EXTRACTED_CHARS) {
                text = text.substring(0, MAX_EXTRACTED_CHARS);
            }
            return new PdfExtractionResult(pageCount, text == null ? "" : text.trim());
        } catch (IOException e) {
            throw new BusinessException("El archivo no es un PDF válido o está dañado");
        }
    }

    public record PdfExtractionResult(int pageCount, String extractedText) {
    }
}
