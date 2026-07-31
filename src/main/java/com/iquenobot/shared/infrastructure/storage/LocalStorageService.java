package com.iquenobot.shared.infrastructure.storage;

import com.iquenobot.shared.domain.service.StorageService;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.util.Sha256Util;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local filesystem storage implementation (development / single-node deployments).
 * Files are written under the configured upload path (app.upload.path) and served
 * statically through /uploads/**.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LocalStorageService implements StorageService {

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8085}")
    private String baseUrl;

    private Path rootDir;

    @PostConstruct
    void init() {
        this.rootDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootDir);
            log.info("LocalStorageService initialized at: {}", this.rootDir);
        } catch (IOException e) {
            throw new BusinessException("Could not create storage directory: " + this.rootDir);
        }
    }

    @Override
    public StoredObject store(byte[] data, String objectKey, String contentType) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            log.info("Stored object: {} ({} bytes)", objectKey, data.length);
            return new StoredObject(objectKey, resolvePublicUrl(objectKey), data.length, Sha256Util.hash(data));
        } catch (IOException e) {
            throw new BusinessException("Could not store file: " + objectKey);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        Path target = resolve(objectKey);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new BusinessException("No se pudo leer el archivo almacenado: " + objectKey);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
            log.info("Deleted object: {}", objectKey);
        } catch (IOException e) {
            log.warn("Could not delete object: {}", objectKey, e);
        }
    }

    @Override
    public boolean exists(String objectKey) {
        return Files.exists(resolve(objectKey));
    }

    @Override
    public String resolvePublicUrl(String objectKey) {
        return baseUrl + "/uploads/" + objectKey;
    }

    private Path resolve(String objectKey) {
        Path target = this.rootDir.resolve(objectKey).normalize();
        if (!target.startsWith(this.rootDir)) {
            throw new BusinessException("Invalid storage key outside root directory");
        }
        return target;
    }
}
