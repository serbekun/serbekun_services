package com.serbekun.ss.domain.models;

import java.util.Map;
import java.util.UUID;

/**
 * 
 */
public class UploadedFiles {
    
    private final Map<UUID, UploadedFile> uploadedFiles;

    public UploadedFiles(Map<UUID, UploadedFile> uploadedFiles) {
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

    public synchronized Map<UUID, UploadedFile> getAllUploadedFiles() {
        return Map.copyOf(uploadedFiles);
    }
}
