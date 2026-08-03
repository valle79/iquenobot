package com.iquenobot.shared.application;

import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.infrastructure.storage.LocalStorageService;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        LocalStorageService storageService = new LocalStorageService();
        ReflectionTestUtils.setField(storageService, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "baseUrl", "http://localhost:8085");
        ReflectionTestUtils.setField(storageService, "rootDir", tempDir.toAbsolutePath().normalize());
        fileUploadService = new FileUploadService(storageService);
    }

    @Test
    void uploadImage_shouldReturnUrl_whenValidImage() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test-image-content".getBytes());

        String url = fileUploadService.uploadImage(file);

        assertNotNull(url);
        assertTrue(url.startsWith("http://localhost:8085/uploads/images/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void uploadImage_shouldThrow_whenInvalidType() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test-content".getBytes());

        assertThrows(BusinessException.class, () -> fileUploadService.uploadImage(file));
    }

    @Test
    void uploadDocument_shouldReturnUrl_whenValidDocument() {
        MultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", "document-content".getBytes());

        DocumentUploadResult result = fileUploadService.uploadDocument(file);

        assertNotNull(result.url());
        assertTrue(result.url().endsWith(".txt"));
        assertNull(result.pageCount());
        assertNull(result.extractedText());
    }

    @Test
    void uploadDocument_shouldExtractText_whenValidPdf() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", createValidPdf("Hola mundo del catalogo"));

        DocumentUploadResult result = fileUploadService.uploadDocument(file);

        assertNotNull(result.url());
        assertTrue(result.url().endsWith(".pdf"));
        assertEquals(1, result.pageCount());
        assertNotNull(result.extractedText());
        assertTrue(result.extractedText().contains("Hola mundo"));
    }

    @Test
    void uploadDocument_shouldThrow_whenPdfIsCorrupt() {
        MultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "not-a-real-pdf".getBytes());

        assertThrows(BusinessException.class, () -> fileUploadService.uploadDocument(file));
    }

    @Test
    void uploadAttachment_shouldAcceptAnyType() {
        MultipartFile file = new MockMultipartFile(
                "file", "data.bin", "application/octet-stream", "binary-content".getBytes());

        String url = fileUploadService.uploadAttachment(file);

        assertNotNull(url);
    }

    @Test
    void uploadAttachment_shouldThrow_whenFileExceedsMaxSize() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile(
                "file", "large.bin", "application/octet-stream", largeContent);

        assertThrows(BusinessException.class, () -> fileUploadService.uploadAttachment(file));
    }

    @Test
    void deleteFile_shouldNotThrow_whenUrlIsNull() {
        assertDoesNotThrow(() -> fileUploadService.deleteFile(null));
        assertDoesNotThrow(() -> fileUploadService.deleteFile(""));
    }

    private byte[] createValidPdf(String text) throws Exception {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(text));
        document.close();
        return out.toByteArray();
    }
}
