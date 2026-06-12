package com.serbekun.ss.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.UploadedFile;
import com.serbekun.ss.domain.models.UploadedFiles;

public class UploadedFilesRepositoryImpl implements UploadFilesRepository {

    private static final Logger log = LoggerFactory.getLogger(UploadedFilesRepositoryImpl.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private final UploadedFiles uploadedFiles;

    public UploadedFilesRepositoryImpl(Path file) {
        this.file = file;
        this.uploadedFiles = new UploadedFiles(load());
    }

    private Map<UUID, UploadedFile> load() {
        File f = file.toFile();

        if (!Files.exists(file) || f.length() == 0) {
            log.info("File does not exist or is empty. Starting with empty storage.");
            return new LinkedHashMap<>();
        }

        try {
            return mapper.readValue(f, new TypeReference<Map<UUID, UploadedFile>>() {});
        } catch (IOException e) {
            log.error("Failed to load uploaded files from {}", file, e);
            return new LinkedHashMap<>();
        }
    }

    @Override
    public UploadedFiles UploadedFiles() {
        return uploadedFiles;
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), uploadedFiles.getAllUploadedFiles());
            
            log.debug("Successfully saved {} uploaded files", uploadedFiles.getAllUploadedFiles().size());
        } catch (IOException e) {
            log.error("Failed to save uploaded files to {}", file, e);
        }
    }
}