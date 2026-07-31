package com.iquenobot.shared.domain.service;

/**
 * Storage abstraction for document files. The business layer depends only on
 * this interface, never on a concrete provider (local disk, S3, R2, MinIO, ...).
 */
public interface StorageService {

    /**
     * Stores raw bytes under the given object key.
     *
     * @return metadata about the stored object (key, public URL, size, sha-256 hash)
     */
    StoredObject store(byte[] data, String objectKey, String contentType);

    /**
     * Loads the raw bytes of the stored object.
     */
    byte[] load(String objectKey);

    /**
     * Deletes the stored object (no-op if it does not exist).
     */
    void delete(String objectKey);

    /**
     * Returns true if the object exists in the storage.
     */
    boolean exists(String objectKey);

    /**
     * Resolves a URL that can be used to download/send the object (public URL,
     * or signed URL when the provider requires it).
     */
    String resolvePublicUrl(String objectKey);

    record StoredObject(String objectKey, String publicUrl, long size, String sha256Hash) {}
}
