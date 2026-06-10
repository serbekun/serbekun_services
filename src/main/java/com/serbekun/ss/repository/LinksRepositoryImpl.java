package com.serbekun.ss.repository;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.domain.models.Links;
import com.serbekun.ss.domain.models.Link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinksRepositoryImpl implements LinksRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    
    private final Links links;

    private final Path file;

    private static final Logger log = 
        LoggerFactory.getLogger(LinksRepositoryImpl.class);


    public LinksRepositoryImpl(Path file) {
        this.file = file;
        this.links = new Links(load());
    }

    @Override
    public Links getLinks() {
        return links;
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

        File f = file.toFile();

        try {
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(f, links.getAllLinks());
        } catch (IOException e) {
            log.error("Error save links: {}", e);
        }
    }
}