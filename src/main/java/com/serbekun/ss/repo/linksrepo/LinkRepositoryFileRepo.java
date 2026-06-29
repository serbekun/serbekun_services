package com.serbekun.ss.repo.linksrepo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.LinkRepository;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public class LinkRepositoryFileRepo implements AutoSavable {

    private static final Logger log = LoggerFactory.getLogger(LinkRepositoryFileRepo.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private LinkRepositoryReadInterface readInterface;

    public LinkRepositoryFileRepo(Path file) {
        this.file = file;
    }

    public void setReadInterface(LinkRepositoryReadInterface readInterface) {
        this.readInterface = readInterface;
    }

    public Map<UUID, LinkRepository> load() {
        File f = file.toFile();
        if (!f.exists()) {
            log.info("Repository file not found, starting with empty map: {}", file);
            return new HashMap<>();
        }
        try {
            Map<UUID, LinkRepository> loaded = mapper.readValue(
                f,
                new TypeReference<Map<UUID, LinkRepository>>() {}
            );
            log.info("Loaded {} link repositories from {}", loaded.size(), file);
            return loaded;
        } catch (IOException e) {
            log.error("Failed to load link repositories from {}. Starting with empty map.", file, e);
            return new HashMap<>();
        }
    }

    @Override
    public synchronized void save() {
        if (readInterface == null) {
            log.error("LinkRepositoryReadInterface is null, cannot save data");
            log.error("Set it via setReadInterface()");
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), readInterface.getRepositoriesData());
            log.debug("Link repositories saved to {}", file);
        } catch (IOException e) {
            log.error("Failed to save link repositories to {}", file, e);
        }
    }
}
