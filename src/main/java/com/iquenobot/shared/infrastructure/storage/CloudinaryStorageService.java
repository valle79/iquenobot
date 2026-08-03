package com.iquenobot.shared.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.iquenobot.shared.domain.service.StorageService;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.util.Sha256Util;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Cloudinary storage implementation (cloud hosting for images, PDFs and
 * attachments). Enabled when app.storage.provider=cloudinary.
 *
 * Files are stored under a common folder prefix, so everything stays grouped
 * in the Cloudinary Media Library.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "cloudinary")
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageService implements StorageService {

    private static final String FOLDER_PREFIX = "iquenobot";
    private static final Map<String, String> EXTENSION_RESOURCE_TYPES = Map.ofEntries(
            Map.entry("jpg", "image"), Map.entry("jpeg", "image"), Map.entry("png", "image"),
            Map.entry("webp", "image"), Map.entry("gif", "image"), Map.entry("svg", "image"),
            Map.entry("mp4", "video"), Map.entry("webm", "video"), Map.entry("mov", "video"),
            Map.entry("pdf", "raw"), Map.entry("txt", "raw"), Map.entry("csv", "raw"),
            Map.entry("doc", "raw"), Map.entry("docx", "raw"), Map.entry("xls", "raw"),
            Map.entry("xlsx", "raw"), Map.entry("mp3", "raw"), Map.entry("wav", "raw"),
            Map.entry("m4a", "raw"), Map.entry("ogg", "raw"));

    private final StorageProperties properties;

    private Cloudinary cloudinary;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @PostConstruct
    void init() {
        StorageProperties.Cloudinary config = properties.getCloudinary();
        if (!hasText(config.getCloudName()) || !hasText(config.getApiKey()) || !hasText(config.getApiSecret())) {
            throw new BusinessException(
                    "Cloudinary storage is not configured. Set app.storage.cloudinary.cloud-name, api-key and api-secret");
        }
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", config.getCloudName(),
                "api_key", config.getApiKey(),
                "api_secret", config.getApiSecret()));
        log.info("CloudinaryStorageService initialized (cloud_name={})", config.getCloudName());
    }

    @Override
    public StoredObject store(byte[] data, String objectKey, String contentType) {
        String publicId = publicIdFor(objectKey);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", resourceTypeFor(objectKey),
                    "overwrite", true));
            String url = (String) result.get("secure_url");
            log.info("Stored Cloudinary object: {} ({} bytes)", publicId, data.length);
            return new StoredObject(objectKey, url, data.length, Sha256Util.hash(data));
        } catch (IOException e) {
            throw new BusinessException("Could not store file: " + objectKey);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolvePublicUrl(objectKey)))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new BusinessException("No se pudo leer el archivo almacenado: " + objectKey);
            }
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("No se pudo leer el archivo almacenado: " + objectKey);
        }
    }

    @Override
    public void delete(String objectKey) {
        String publicId = publicIdFor(objectKey);
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceTypeFor(objectKey),
                    "invalidate", true));
            log.info("Deleted Cloudinary object: {} (result={})", publicId, result.get("result"));
        } catch (IOException e) {
            log.warn("Could not delete Cloudinary object {}: {}", publicId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            cloudinary.api().resource(publicIdFor(objectKey), ObjectUtils.emptyMap());
            return true;
        } catch (Exception e) {
            if (isNotFound(e)) {
                return false;
            }
            throw new BusinessException("No se pudo verificar el archivo almacenado: " + objectKey);
        }
    }

    @Override
    public String resolvePublicUrl(String objectKey) {
        String publicId = publicIdFor(objectKey);
        return cloudinary.url()
                .resourceType(resourceTypeFor(objectKey))
                .format(extensionOf(objectKey))
                .generate(publicId);
    }

    /**
     * Folder + file name without extension, e.g. "iquenobot/images/abc-123".
     */
    private String publicIdFor(String objectKey) {
        String withoutExtension = stripExtension(objectKey);
        return FOLDER_PREFIX + "/" + withoutExtension;
    }

    /**
     * Maps a storage key extension to the Cloudinary resource type.
     * Defaults to "raw" (PDFs, documents, unknown types).
     */
    private String resourceTypeFor(String objectKey) {
        return EXTENSION_RESOURCE_TYPES.getOrDefault(extensionOf(objectKey).toLowerCase(), "raw");
    }

    private String extensionOf(String objectKey) {
        if (objectKey == null) {
            return "";
        }
        int dot = objectKey.lastIndexOf('.');
        return dot >= 0 && dot < objectKey.length() - 1 ? objectKey.substring(dot + 1) : "";
    }

    private String stripExtension(String objectKey) {
        String extension = extensionOf(objectKey);
        return extension.isEmpty() ? objectKey : objectKey.substring(0, objectKey.length() - extension.length() - 1);
    }

    private boolean isNotFound(Exception e) {
        return e.getClass().getSimpleName().toLowerCase().contains("notfound");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
