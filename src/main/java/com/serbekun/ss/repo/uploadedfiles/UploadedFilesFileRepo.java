package com.serbekun.ss.repo.uploadedfiles;

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
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public class UploadedFilesFileRepo implements AutoSavable {

    private static final Logger log = LoggerFactory.getLogger(UploadedFilesFileRepo.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private UploadedFilesReadInterface uploadedFilesReadInterface;

    public UploadedFilesFileRepo(Path file) {
        this.file = file;
    }

    public void setUploadedFilesReadInterface(UploadedFilesReadInterface uploadedFilesReadInterface) {
        this.uploadedFilesReadInterface = uploadedFilesReadInterface;
    }

    public Map<UUID, UploadedFile> load() {
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
    public void save() {
        if (uploadedFilesReadInterface == null) {
            log.error("uploadedFilesReadInterface Object is null cannot save data");
            log.error("Setup uploadedFilesReadInterface use setUploadedFilesReadInterface()");
            log.error("for setup uploadedFilesReadInterface.");
        }
        
        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), uploadedFilesReadInterface.getUploadedFilesData());
            
            log.debug("Successfully saved {} uploaded files", uploadedFilesReadInterface.getUploadedFilesData().size());
        } catch (IOException e) {
            log.error("Failed to save uploaded files to {}", file, e);
        }
    }
}