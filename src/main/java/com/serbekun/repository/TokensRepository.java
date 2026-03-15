package com.serbekun.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.service.auth.Endpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for manage tokens repository
 */
public class TokensRepository {

    private final ObjectMapper mapper = new ObjectMapper();

    private final String tokensStorageFile;

    private static final Logger log = 
        LoggerFactory.getLogger(TokensRepository.class);

    public TokensRepository(String tokensStorageFile) {
        this.tokensStorageFile = tokensStorageFile;
    }

    /**
     * Load all tokens and index them by token string.
     * If the storage file is missing, an empty map is returned.
     */
    public Map<String, List<Endpoints>> load() {

        try {
            var result = mapper.readValue(
                    new File(tokensStorageFile),
                    new TypeReference<Map<String, List<Endpoints>>>() {}
            );

            return result;

        } catch (IOException e) {
            log.info("Error load tokens: {}", e);
            log.info("starting from new hashmap");
            return new LinkedHashMap<String, List<Endpoints>>();
        }
    
    }

    /**
     * 
     * @param tokens object to save tokens map
     */
    public void save(Map<String, List<Endpoints>> tokens) {
        
        try {
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(new File(tokensStorageFile), tokens);
        } catch (IOException e) {
            log.error("Error save tokens: {}", e);
        }
    }
}