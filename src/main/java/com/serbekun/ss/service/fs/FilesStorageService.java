package com.serbekun.ss.service.fs;

import java.util.UUID;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Class that provide lo level IO to uploaded files.
 */
public class FilesStorageService {

    private final Path folder;

    public FilesStorageService(Path folder) {
        this.folder = folder;
    }

    private Path formatPath(UUID uuid) {
        String uuidString = uuid.toString();
        String basePath = folder.toString();
        return Paths.get(basePath + uuidString);
    }

    /**
     * Write bytes to file use basepath selected in constructor.
     * 
     * @param fileRawBytes raw bytes of file which will be wrote.
     * @param uuid uuid of file that uuid will be used as file name.
     */
    public void AddFile(byte[] fileRawBytes, UUID uuid) {
        Path path = formatPath(uuid);

        try {
            Files.write(path, fileRawBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void RemoveFile(UUID uuid) {
        Path filePath = formatPath(uuid);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                System.out.println("File deleted successfully.");
            } else {
                System.out.println("File does not exist.");
            }
        } catch (IOException e) {
            System.err.println("Failed to delete the file: " + e.getMessage());
        }
    }

    public byte[] GetFile(UUID uuid) {
        Path path = formatPath(uuid);
        
        try {
            byte[] fileBytes = Files.readAllBytes(path);
            return fileBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
