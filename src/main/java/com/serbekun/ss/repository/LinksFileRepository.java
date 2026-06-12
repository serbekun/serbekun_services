package com.serbekun.ss.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinksRepositoryReadInterface;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public class LinksFileRepository implements AutoSavable {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private LinksRepositoryReadInterface linksRepositoryReadInterface;

    private static final Logger log = 
        LoggerFactory.getLogger(LinksFileRepository.class);


    public LinksFileRepository(Path file) {
        this.file = file;
    }

    public void setLinksRepositoryReadInterface(LinksRepositoryReadInterface linksRepositoryReadInterface) {
        this.linksRepositoryReadInterface = linksRepositoryReadInterface;
    }

    public Map<UUID, Link> load() {

        File f = file.toFile();

        if (!f.exists()) {
            return new HashMap<>();
        }

        try {

            if (!Files.exists(file)) {
                log.info("File not found start from new hashmap");
                return new LinkedHashMap<UUID, Link>();
            }

            var result = mapper.readValue(
                    f,
                    new TypeReference<Map<UUID, Link>>() {}
            );

            return result;

        } catch (IOException e) {
            log.error("Error load links: {}", e);
            log.info("starting from new hashmap");
            return new LinkedHashMap<UUID, Link>();
        }
    
    }

    @Override
    public void save() {
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), linksRepositoryReadInterface.getLinksData());
        } catch (IOException e) {
            log.error("Error save links: {}", e);
        }
    }
}