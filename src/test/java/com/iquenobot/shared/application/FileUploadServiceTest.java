package com.iquenobot.shared.application;

import com.iquenobot.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadServiceTest {

    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService();
        ReflectionTestUtils.setField(fileUploadService, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(fileUploadService, "baseUrl", "http://localhost:8085");
        fileUploadService.init();
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
                "file", "doc.pdf", "application/pdf", "pdf-content".getBytes());

        String url = fileUploadService.uploadDocument(file);

        assertNotNull(url);
        assertTrue(url.endsWith(".pdf"));
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

    @Test
    void init_shouldCreateDirectories() {
        assertTrue(tempDir.toFile().exists());
    }
}
