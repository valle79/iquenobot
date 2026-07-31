package com.iquenobot.shared.infrastructure.storage;

import com.iquenobot.shared.domain.service.StorageService;
import com.iquenobot.shared.exception.BusinessException;
import com.iquenobot.shared.util.Sha256Util;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

/**
 * S3-compatible storage implementation (AWS S3, Cloudflare R2, MinIO, Google
 * Cloud Storage via the S3 interop API). Enabled when app.storage.provider=s3.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final StorageProperties properties;

    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    void init() {
        StorageProperties.S3 s3 = properties.getS3();
        var builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        if (hasText(s3.getEndpoint())) {
            URI endpoint = URI.create(s3.getEndpoint());
            builder.endpointOverride(endpoint)
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
            presignerBuilder.endpointOverride(endpoint)
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        this.s3Client = builder.build();
        this.presigner = presignerBuilder.build();
        log.info("S3StorageService initialized (bucket={}, region={})", s3.getBucket(), s3.getRegion());
    }

    @Override
    public StoredObject store(byte[] data, String objectKey, String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(properties.getS3().getBucket())
                .key(objectKey)
                .contentType(contentType)
                .build(), RequestBody.fromBytes(data));
        log.info("Stored S3 object: {} ({} bytes)", objectKey, data.length);
        return new StoredObject(objectKey, resolvePublicUrl(objectKey), data.length, Sha256Util.hash(data));
    }

    @Override
    public byte[] load(String objectKey) {
        try {
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(properties.getS3().getBucket())
                    .key(objectKey)
                    .build());
            return response.readAllBytes();
        } catch (S3Exception | IOException e) {
            throw new BusinessException("No se pudo leer el archivo almacenado: " + objectKey);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getS3().getBucket())
                    .key(objectKey)
                    .build());
            log.info("Deleted S3 object: {}", objectKey);
        } catch (S3Exception e) {
            log.warn("Could not delete S3 object {}: {}", objectKey, e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getS3().getBucket())
                    .key(objectKey)
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || e.statusCode() == 403) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public String resolvePublicUrl(String objectKey) {
        StorageProperties.S3 s3 = properties.getS3();
        if (hasText(s3.getPublicBaseUrl())) {
            return s3.getPublicBaseUrl() + "/" + objectKey;
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(s3.getPresignExpiryMinutes()))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(s3.getBucket())
                        .key(objectKey)
                        .build())
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
