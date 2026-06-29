package com.serbekun.ss.repo.uploadedfiles;

import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.UploadedFile;

/**
 * 
 */
public class UploadedFilesRepo implements UploadedFilesReadInterface {
    
    private final Map<UUID, UploadedFile> uploadedFiles;

    /**
     * Creates a new instance of UploadedFilesRepo with the provided map of uploaded files.
     * @param uploadedFiles a map containing the uploaded files, where the key is the UUID of the file and the value is the UploadedFile object
     * @throws IllegalArgumentException if the uploadedFiles map is null
     */
    public UploadedFilesRepo(Map<UUID, UploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    /**
     * Checks if an uploaded file with the specified UUID exists in the repository.
     * @param uuid the UUID of the uploaded file to check for existence
     * @return true if the uploaded file exists, false otherwise
     */
    public synchronized boolean existsUploadedFile(UUID uuid) {
        return uploadedFiles.containsKey(uuid);
    }

    /**
     * Adds a new uploaded file to the repository. If the uploaded file is null or its UUID is null, the method does nothing.
     * @param uploadedFile the UploadedFile object to be added to the repository
     * @throws IllegalArgumentException if the uploadedFile is null or its UUID is null
     */
    public synchronized void addUploadedFile(UploadedFile uploadedFile) {
        if (uploadedFile == null || uploadedFile.uuid() == null) {
            return;
        }
        uploadedFiles.put(uploadedFile.uuid(), uploadedFile);
    }

    /**
     * Retrieves an uploaded file from the repository based on its UUID.
     * @param uuid the UUID of the uploaded file to retrieve
     * @return the UploadedFile object if found, or null if not found or if the UUID is null
     */
    public synchronized UploadedFile getUploadedFile(UUID uuid) {
        return (uuid == null) ? null : uploadedFiles.get(uuid);
    }

    /**
     * Updates an existing uploaded file in the repository. If the UUID is null, the method does nothing.
     * @param uuid the UUID of the uploaded file to update
     * @param newUploadedFile the new UploadedFile object to replace the existing one
     * @throws IllegalArgumentException if the newUploadedFile is null or its UUID is null
     */
    public synchronized void updateUploadedFile(UUID uuid, UploadedFile newUploadedFile) {
        removeUploadedFile(uuid);
        addUploadedFile(newUploadedFile);
    }

    /**
     * Removes an uploaded file from the repository based on its UUID.
     * @param uuid the UUID of the uploaded file to remove
     * @return the removed UploadedFile object if it existed, or null if it did not exist or if the UUID is null
     */
    public synchronized UploadedFile removeUploadedFile(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return uploadedFiles.remove(uuid);
    }

    /**
     * Returns a copy of the map containing all uploaded files in the repository.
     * @return a map of all uploaded files
    */
    @Override
    public synchronized Map<UUID, UploadedFile> getUploadedFilesData() {
        return Map.copyOf(uploadedFiles);
    }
}
