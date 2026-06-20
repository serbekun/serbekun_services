package com.serbekun.ss.repo.uploadedfiles;

import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.UploadedFile;

/**
 * Interface for provide to {@link com.serbekun.ss.repo.uploadedfiles.UploadedFilesFileRepo}
 * File Repository class only read access
 */
public interface UploadedFilesReadInterface {
    /**
     * @return All uploaded files metadata.
     */
    Map<UUID, UploadedFile> getUploadedFilesData();
}
