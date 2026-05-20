package com.serbekun.ss.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.core.LocalTokens;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LocalTokensRepository implements AutoSavable {

    private static final Logger log = LoggerFactory.getLogger(LocalTokensRepository.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file;
    private final LocalTokens tokens;

    public LocalTokensRepository(Path file) {
        this(file, "");
    }

    public LocalTokensRepository(Path file, String prefix) {
        this.file = file;
        this.tokens = new LocalTokens(load(), prefix);
    }

    public LocalTokens getTokens() {
        return tokens;
    }

    @Override
    public synchronized void save() {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), tokens.getAllTokensSnapshot());

            log.debug("Tokens successfully saved to {}", file);
        } catch (IOException e) {
            log.error("Failed to save tokens to {}", file, e);
        }
    }

    private Map<String, String> load() {
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
}
