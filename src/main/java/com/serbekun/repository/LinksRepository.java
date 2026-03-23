package com.serbekun.repository;

// java imports
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.LinkedHashMap;

// jackson imports
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// serbekun imports
import com.serbekun.core.Links.Link;
import com.serbekun.core.Links;
import com.serbekun.service.autosave.interfaces.AutoSavable;

// slf4j imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for manage links repository
 */
public class LinksRepository implements AutoSavable {

    /**
     * {@link com.fasterxml.jackson.databind.ObjectMapper} Jackson mapper object for work with json.
     */
    private final ObjectMapper mapper = new ObjectMapper();
    
    /**
     * {@link com.serbekun.core.Links} object for
     *  load to him data from file
     *  and get data that will be saved.
     */
    private final Links links;

    /**
     * path to file where will be load and save data.
     */
    private final Path file;

    /**
     * slf4j logger for logging repository
     */
    private static final Logger log = 
        LoggerFactory.getLogger(LinksRepository.class);


    /**
     * 
     * @param file path to file where will be load and save data.
     */ 
    public LinksRepository(Path file) {
        this.file = file;
        this.links = new Links(load());
    }

    public Links getLinks() {
        return links;
    }

    /**
     * Load all links and index them by UUID.
     * If the storage file is missing, an empty map is returned.
     */
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

    /**
     * 
     * @param links object to save {@link core.Links.Link} 
     */
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