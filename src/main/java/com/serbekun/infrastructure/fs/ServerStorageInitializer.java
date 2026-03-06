package com.serbekun.infrastructure.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStorageInitializer {

    private static final Logger log =
        LoggerFactory.getLogger(ServerStorageInitializer.class);

    /**
     * 
     * @param root
     */
    public void initialize(Path root) {

        log.info("initialize server storage folder");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            log.error("Error initialize server storage folders: {}", e);
        }
    }
}