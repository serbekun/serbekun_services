package com.serbekun.ss.domain.models;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for provide to {@link com.serbekun.ss.repository.UploadedFilesFileRepo}
 * File Repository class only read access
 */
public interface UploadedFilesReadInterface {
    /**
     * @return All uploaded files metadata.
     */
    Map<UUID, UploadedFile> getUploadedFilesData();
}
