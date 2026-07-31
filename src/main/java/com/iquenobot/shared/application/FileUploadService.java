package com.iquenobot.shared.application;

import com.iquenobot.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileUploadService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/csv");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8085}")
    private String baseUrl;

    private Path uploadDir;

    @PostConstruct
    void init() {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
            log.info("File upload directory created at: {}", this.uploadDir);
        } catch (IOException e) {
            throw new BusinessException("Could not create upload directory: " + this.uploadDir);
        }
    }

    public String uploadImage(MultipartFile file) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
        return storeFile(file, "images");
    }

    public String uploadDocument(MultipartFile file) {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_FILE_SIZE);
        return storeFile(file, "documents");
    }

    public String uploadAttachment(MultipartFile file) {
        validateFile(file, null, MAX_FILE_SIZE);
        return storeFile(file, "attachments");
    }

    /**
     * Persiste bytes crudos de un media (p.ej. descargado de WhatsApp).
     * Subdirectorio según el tipo mime.
     */
    public String uploadBytes(byte[] data, String originalFilename, String mimeType) {
        if (data == null || data.length == 0) {
            throw new BusinessException("No hay contenido para almacenar");
        }
        if (data.length > MAX_FILE_SIZE) {
            throw new BusinessException("El archivo excede el tamaño máximo permitido de " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        }

        String subdirectory = resolveSubdirectory(mimeType);
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (extension.isBlank() && mimeType != null) {
            extension = extensionForMimeType(mimeType);
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = this.uploadDir.resolve(subdirectory).resolve(filename);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            log.info("File stored from bytes: {} (original: {}, mime: {})", filename, originalFilename, mimeType);

            return baseUrl + "/uploads/" + subdirectory + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Could not store file: " + originalFilename);
        }
    }

    private String resolveSubdirectory(String mimeType) {
        if (mimeType != null && mimeType.startsWith("image/")) {
            return "images";
        }
        return "attachments";
    }

    private String extensionForMimeType(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/svg+xml" -> ".svg";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "audio/ogg" -> ".ogg";
            case "audio/mpeg", "audio/mp3" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/mp4", "audio/x-m4a" -> ".m4a";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            default -> "";
        };
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        String relativePath = fileUrl.replace(baseUrl + "/uploads/", "");
        Path filePath = this.uploadDir.resolve(relativePath).normalize();

        if (!filePath.startsWith(this.uploadDir)) {
            throw new BusinessException("Cannot delete file outside upload directory");
        }

        try {
            Files.deleteIfExists(filePath);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath, e);
        }
    }

    private String storeFile(MultipartFile file, String subdirectory) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path targetPath = this.uploadDir.resolve(subdirectory).resolve(filename);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored: {} (original: {})", filename, originalFilename);

            return baseUrl + "/uploads/" + subdirectory + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Could not store file: " + originalFilename);
        }
    }

    private void validateFile(MultipartFile file, Set<String> allowedTypes, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Debe seleccionar un archivo");
        }

        if (file.getSize() > maxSize) {
            throw new BusinessException("El archivo excede el tamaño máximo permitido de " + (maxSize / (1024 * 1024)) + "MB");
        }

        if (allowedTypes != null && file.getContentType() != null && !allowedTypes.contains(file.getContentType())) {
            throw new BusinessException("Tipo de archivo no permitido: " + file.getContentType());
        }
    }
}
