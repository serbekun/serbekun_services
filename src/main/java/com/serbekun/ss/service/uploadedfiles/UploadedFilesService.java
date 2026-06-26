package com.serbekun.ss.service.uploadedfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.UploadedFile;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesRepo;

/**
 * Service for managing uploaded files: CRUD operations on metadata
 * and raw file storage on disk.
 */
public class UploadedFilesService {
 
    // region constants

    /** Logger for the uploaded files service. */
    private static final Logger log = LoggerFactory.getLogger(UploadedFilesService.class);

    /** The repository for managing uploaded file metadata. */
    private final UploadedFilesRepo repo;
    /** The directory where raw file content is stored. */
    private final Path rawFilesDir;

    // endregion

    public UploadedFilesService(UploadedFilesRepo repo, Path rawFilesDir) {
        this.repo = repo;
        this.rawFilesDir = rawFilesDir;
        ensureRawFilesDir();
    }

    /**
     * Ensure the raw files directory exists.
     */
    private void ensureRawFilesDir() {
        try {
            Files.createDirectories(rawFilesDir);
        } catch (IOException e) {
            log.error("Failed to create raw files directory: {}", rawFilesDir, e);
        }
    }

    // region CRUD operations

    /**
     * Upload a file: persist metadata and write raw bytes to disk.
     * @return the created {@link UploadedFile} metadata.
     */
    public synchronized UploadedFile uploadFile(UploadedFile meta, byte[] content) throws IOException {
        if (meta == null || meta.uuid() == null) {
            throw new IllegalArgumentException("UploadedFile metadata must have a non-null uuid");
        }
        if (content == null) {
            throw new IllegalArgumentException("File content must not be null");
        }

        Path target = rawFilesDir.resolve(meta.uuid().toString());
        Files.write(target, content);

        repo.addUploadedFile(meta);
        log.info("Uploaded file '{}' (uuid={})", meta.name(), meta.uuid());
        return meta;
    }

    /**
     * Verify that the token matches the file's token.
     * @return the {@link UploadedFile} if found and token matches, null otherwise.
     */
    public synchronized UploadedFile verifyFileAccess(UUID uuid, String token) {
        UploadedFile f = repo.getUploadedFile(uuid);
        if (f == null || token == null || !token.equals(f.token())) {
            return null;
        }
        return f;
    }

    /** Get metadata for a single uploaded file (no token check). */
    public synchronized UploadedFile getFileMetadata(UUID uuid) {
        return repo.getUploadedFile(uuid);
    }

    /** Get metadata for all uploaded files. */
    public synchronized Map<UUID, UploadedFile> getAllFilesMetadata() {
        return repo.getUploadedFilesData();
    }

    /** Read raw file content from disk. Returns null if the file does not exist. */
    public synchronized byte[] getFileContent(UUID uuid) throws IOException {
        Path source = rawFilesDir.resolve(uuid.toString());
        if (!Files.exists(source)) {
            return null;
        }
        return Files.readAllBytes(source);
    }

    /** Check whether a file with the given UUID exists in the repository. */
    public synchronized boolean exists(UUID uuid) {
        return repo.existsUploadedFile(uuid);
    }

    /**
     * Delete an uploaded file (both raw bytes and metadata).
     * Requires the correct token that was issued at upload time.
     *
     * @return HTTP-like status: 204 on success, 403 if token mismatch, 404 if not found.
     */
    public synchronized int deleteFile(UUID uuid, String token) {
        UploadedFile existing = repo.getUploadedFile(uuid);
        if (existing == null) {
            return 404;
        }
        if (token == null || !token.equals(existing.token())) {
            return 403;
        }

        // Delete raw file
        Path source = rawFilesDir.resolve(uuid.toString());
        try {
            Files.deleteIfExists(source);
        } catch (IOException e) {
            log.error("Failed to delete raw file for uuid={}", uuid, e);
            return 500;
        }

        repo.removeUploadedFile(uuid);
        log.info("Deleted uploaded file uuid={}", uuid);
        return 204;
    }

    // region Expiration operations

    /**
     * Remove all files whose {@code expiredTime} is in the past.
     * Called periodically by {@link UploadedFilesCleanupService}.
     *
     * @return number of expired files removed.
     */
    public synchronized int deleteExpiredFiles() {
        long now = System.currentTimeMillis();
        int removed = 0;

        for (UploadedFile f : repo.getUploadedFilesData().values()) {
            if (f.expiredTime() > 0 && f.expiredTime() < now) {
                Path source = rawFilesDir.resolve(f.uuid().toString());
                try {
                    Files.deleteIfExists(source);
                } catch (IOException e) {
                    log.error("Failed to delete expired raw file uuid={}", f.uuid(), e);
                    continue;
                }
                repo.removeUploadedFile(f.uuid());
                removed++;
                log.info("Removed expired file '{}' (uuid={})", f.name(), f.uuid());
            }
        }

        if (removed > 0) {
            log.info("Cleaned up {} expired uploaded file(s)", removed);
        }
        return removed;
    }
}
