// TODO add logger

package com.serbekun.ss.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.core.LocalTokens;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LocalTokensRepository implements AutoSavable {

    private final ObjectMapper mapper = new ObjectMapper();

    private final Path file;
    private final LocalTokens tokens;

    public LocalTokensRepository(Path file) {
        this.file = file;
        this.tokens = new LocalTokens(load());
    }

    public LocalTokens getTokens() {
        return tokens;
    }

    @Override
    public synchronized void save() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), tokens.getAllTokensSnapshot());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> load() {
        File f = file.toFile();

        if (!f.exists()) {
            return new HashMap<>();
        }

        try {
            return mapper.readValue(
                f,
                new TypeReference<Map<String, String>>() {}
            );
        } catch (IOException e) {
            
            return new HashMap<>();
        }
    }
}