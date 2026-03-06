package com.serbekun.repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.IOException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.core.Links.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for manage links repository
 */
public class LinksRepository {

    private final ObjectMapper mapper = new ObjectMapper();

    private final String linksStorageFile;

    private static final Logger log = 
        LoggerFactory.getLogger(LinksRepository.class);

    public LinksRepository(String linksStorageFile) {
        this.linksStorageFile = linksStorageFile;
    }

    /**
     * Load all links and index them by UUID.
     * If the storage file is missing, an empty map is returned.
     */
    public Map<UUID, Link> load() {

        try {
            var result = mapper.readValue(
                    new File(linksStorageFile),
                    new TypeReference<Map<UUID, Link>>() {}
            );

            return result;

        } catch (IOException e) {
            log.info("Error load links: {}", e);
            log.info("starting from new hashmap");
            return new LinkedHashMap<UUID, Link>();
        }
    
    }

    /**
     * 
     * @param links object to save {@link core.Links.Link} 
     */
    public void save(Map<UUID, Link> links) {
        
        try {
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(linksStorageFile), links);
        } catch (IOException e) {
            log.error("Error save links: {}", e);
        }
    }
}