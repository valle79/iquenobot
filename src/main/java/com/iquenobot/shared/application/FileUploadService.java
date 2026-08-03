package com.iquenobot.shared.application;

import com.iquenobot.shared.domain.service.StorageService;
import com.iquenobot.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain", "text/csv");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    private final StorageService storageService;

    public String uploadImage(MultipartFile file) {
        validateFile(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
        return storeFile(file, "images");
    }

    public DocumentUploadResult uploadDocument(MultipartFile file) {
        validateFile(file, ALLOWED_DOCUMENT_TYPES, MAX_FILE_SIZE);

        boolean isPdf = "application/pdf".equals(file.getContentType());
        PdfTextExtractor.PdfExtractionResult extraction = null;
        if (isPdf) {
            try {
                extraction = PdfTextExtractor.extract(file.getBytes());
            } catch (IOException e) {
                throw new BusinessException("No se pudo leer el archivo PDF");
            }
        }

        String url = storeFile(file, "documents");
        if (extraction == null) {
            return new DocumentUploadResult(url, null, null);
        }
        return new DocumentUploadResult(url, extraction.pageCount(), extraction.extractedText());
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
        String objectKey = buildKey(subdirectory, originalFilename, mimeType);
        StorageService.StoredObject stored = storageService.store(data, objectKey, mimeType);

        log.info("File stored from bytes: {} (original: {}, mime: {})", objectKey, originalFilename, mimeType);
        return stored.publicUrl();
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        String objectKey = extractObjectKey(fileUrl);
        if (objectKey == null) {
            return;
        }
        storageService.delete(objectKey);
        log.info("File deleted: {}", objectKey);
    }

    private String storeFile(MultipartFile file, String subdirectory) {
        String objectKey = buildKey(subdirectory, file.getOriginalFilename(), file.getContentType());
        try {
            StorageService.StoredObject stored = storageService.store(file.getBytes(), objectKey, file.getContentType());
            log.info("File stored: {} (original: {})", objectKey, file.getOriginalFilename());
            return stored.publicUrl();
        } catch (IOException e) {
            throw new BusinessException("Could not store file: " + file.getOriginalFilename());
        }
    }

    private String buildKey(String subdirectory, String originalFilename, String mimeType) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (extension.isBlank() && mimeType != null) {
            extension = extensionForMimeType(mimeType);
        }
        return subdirectory + "/" + UUID.randomUUID() + extension;
    }

    /**
     * Convierte la URL pública de vuelta a su object key según el proveedor de
     * almacenamiento (local o Cloudinary). Devuelve null si la URL no es
     * reconocida.
     */
    private String extractObjectKey(String fileUrl) {
        int cloudinaryMarker = fileUrl.indexOf("/upload/");
        if (cloudinaryMarker >= 0) {
            String suffix = fileUrl.substring(cloudinaryMarker + "/upload/".length());
            int versionStart = suffix.indexOf("/v");
            if (versionStart == 0) {
                suffix = suffix.substring(suffix.indexOf("/", 2) + 1);
            }
            return suffix;
        }
        int localMarker = fileUrl.indexOf("/uploads/");
        if (localMarker >= 0) {
            return fileUrl.substring(localMarker + "/uploads/".length());
        }
        return null;
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
