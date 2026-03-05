package com.serbekun.infrastructure.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerStorageInitializer {

    /**
     * 
     * @param root
     */
    public void initialize(Path root) {

        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            // TODO add slf4j logger
        }
    }
}