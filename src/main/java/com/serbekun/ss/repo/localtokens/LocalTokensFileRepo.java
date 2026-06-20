package com.serbekun.ss.repo.localtokens;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public class LocalTokensFileRepo implements AutoSavable {

    private static final Logger log = LoggerFactory.getLogger(LocalTokensFileRepo.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file;
    private LocalTokensReadInterface localTokensReadInterface;

    public LocalTokensFileRepo(Path file) {
        this(file, "");
    }

    public LocalTokensFileRepo(Path file, String prefix) {
        this.file = file;
    }

    public void setLocalTokensReadInterface(LocalTokensReadInterface localTokensReadInterface) {
        this.localTokensReadInterface = localTokensReadInterface;
    }

    public Map<String, String> load() {
        File f = file.toFile();
        
        if (!f.exists()) {
            log.info("Token file not found, starting with empty map: {}", file);
            return new HashMap<>();
        }
        
        try {
            Map<String, String> loaded = mapper.readValue(
                f,
                new TypeReference<Map<String, String>>() {}
            );
            log.info("Loaded {} tokens from {}", loaded.size(), file);
            return loaded;
        } catch (IOException e) {
            log.error("Failed to load tokens from {}. Starting with empty map.", file, e);
            return new HashMap<>();
        }
    }

    @Override
    public synchronized void save() {
        if (localTokensReadInterface == null) {
            log.error("localTokenReadInterface Object is null cannot save data");
            log.error("Setup localTokenReadInterface use setLocalTokensReadInterface()");
            log.error("for setup localTokensReadInterface.");
        }

        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), localTokensReadInterface.getTokensData());

            log.debug("Tokens successfully saved to {}", file);
        } catch (IOException e) {
            log.error("Failed to save tokens to {}", file, e);
        }
    }
}
