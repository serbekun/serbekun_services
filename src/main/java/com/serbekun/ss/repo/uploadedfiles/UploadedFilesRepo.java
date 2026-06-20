package com.serbekun.ss.repo.uploadedfiles;

import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.UploadedFile;

/**
 * 
 */
public class UploadedFilesRepo implements UploadedFilesReadInterface {
    
    private final Map<UUID, UploadedFile> uploadedFiles;

    public UploadedFilesRepo(Map<UUID, UploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public synchronized boolean existsUploadedFile(UUID uuid) {
        return uploadedFiles.containsKey(uuid);
    }

    public synchronized void addUploadedFile(UploadedFile uploadedFile) {
        if (uploadedFile == null || uploadedFile.uuid() == null) {
            return;
        }
        uploadedFiles.put(uploadedFile.uuid(), uploadedFile);
    }

    public synchronized UploadedFile getUploadedFile(UUID uuid) {
        return (uuid == null) ? null : uploadedFiles.get(uuid);
    }

    public synchronized void updateUploadedFile(UUID uuid, UploadedFile newUploadedFile) {
        removeUploadedFile(uuid);
        addUploadedFile(newUploadedFile);
    }

    public synchronized UploadedFile removeUploadedFile(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return uploadedFiles.remove(uuid);
    }

    @Override
    public synchronized Map<UUID, UploadedFile> getUploadedFilesData() {
        return Map.copyOf(uploadedFiles);
    }
}
