package com.serbekun.ss.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final int port;
    private final int uploadFileMaxSize;

    @JsonCreator
    public Config(@JsonProperty("port") int port,
    @JsonProperty("upload_file_max_size") int uploadFileMaxSize) {
        this.port = port;
        this.uploadFileMaxSize = uploadFileMaxSize;
    }

    @JsonProperty("port")
    public int getPort() {
        return port;
    }

    @JsonProperty("upload_file_max_size")
    public int getUploadFileMaxSize() {
        return uploadFileMaxSize;
    }

    public static Config load(Path file) {
        File f = file.toFile();

        if (!f.exists()) {
            log.info("Config file not found, creating default config at {}", file);
            Config defaults = new Config(8080, 20971520); // 20MB
            save(defaults, f);
            return defaults;
        }

        try {
            return mapper.readValue(f, Config.class);
        } catch (IOException e) {
            log.error("Error reading config file: {}", e.getMessage());
            log.info("Falling back to default config");
            return new Config(8080, 20971520);  // 20MB
        }
    }

    private static void save(Config config, File file) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, config);
        } catch (IOException e) {
            log.error("Error writing default config file: {}", e.getMessage());
        }
    }
}
