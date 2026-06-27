package com.serbekun.ss.repo.shorturl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.ShortUrl;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public class ShortUrlFileRepo implements AutoSavable {

    private static final Logger log = LoggerFactory.getLogger(ShortUrlFileRepo.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private ShortUrlReadInterface shortUrlReadInterface;

    public ShortUrlFileRepo(Path file) {
        this.file = file;
    }

    public void setShortUrlReadInterface(ShortUrlReadInterface shortUrlReadInterface) {
        this.shortUrlReadInterface = shortUrlReadInterface;
    }

    public Map<String, ShortUrl> load() {
        File f = file.toFile();

        if (!Files.exists(file) || f.length() == 0) {
            log.info("File does not exist or is empty. Starting with empty storage.");
            return new LinkedHashMap<>();
        }

        try {
            return mapper.readValue(f, new TypeReference<Map<String, ShortUrl>>() {});
        } catch (IOException e) {
            log.error("Failed to load short urls from {}", file, e);
            return new LinkedHashMap<>();
        }
    }

    @Override
    public void save() {
        if (shortUrlReadInterface == null) {
            log.error("shortUrlReadInterface Object is null cannot save data");
            log.error("Setup shortUrlReadInterface use setShortUrlReadInterface()");
            log.error("for setup shortUrlReadInterface.");
            return;
        }

        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), shortUrlReadInterface.getShortUrlData());

            log.debug("Successfully saved {} short urls", shortUrlReadInterface.getShortUrlData().size());
        } catch (IOException e) {
            log.error("Failed to save short urls to {}", file, e);
        }
    }
}
