package com.serbekun.ss.repo.burnlink;

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

import com.serbekun.ss.domain.models.BurnLink;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

/**
 * JSON-file persistence for {@link BurnLink} records.
 */
public class BurnLinkFileRepo implements AutoSavable {

    private static final Logger log = LoggerFactory.getLogger(BurnLinkFileRepo.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();
    private BurnLinkReadInterface burnLinkReadInterface;

    public BurnLinkFileRepo(Path file) {
        this.file = file;
    }

    public void setBurnLinkReadInterface(BurnLinkReadInterface burnLinkReadInterface) {
        this.burnLinkReadInterface = burnLinkReadInterface;
    }

    public Map<String, BurnLink> load() {
        File f = file.toFile();

        if (!Files.exists(file) || f.length() == 0) {
            log.info("File does not exist or is empty. Starting with empty storage.");
            return new LinkedHashMap<>();
        }

        try {
            return mapper.readValue(f, new TypeReference<Map<String, BurnLink>>() {});
        } catch (IOException e) {
            log.error("Failed to load burn links from {}", file, e);
            return new LinkedHashMap<>();
        }
    }

    @Override
    public void save() {
        if (burnLinkReadInterface == null) {
            log.error("burnLinkReadInterface Object is null cannot save data");
            log.error("Setup burnLinkReadInterface use setBurnLinkReadInterface()");
            return;
        }

        try {
            Files.createDirectories(file.getParent());
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(file.toFile(), burnLinkReadInterface.getBurnLinkData());

            log.debug("Successfully saved {} burn links", burnLinkReadInterface.getBurnLinkData().size());
        } catch (IOException e) {
            log.error("Failed to save burn links to {}", file, e);
        }
    }
}
