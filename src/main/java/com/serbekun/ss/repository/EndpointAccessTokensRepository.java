package com.serbekun.ss.repository;

// java imports
import java.io.File;
import java.util.Map;
import java.util.List;
import java.nio.file.Path;
import java.io.IOException;
import java.util.LinkedHashMap;

// jackson imports
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.serbekun.ss.core.EndpointsAccessTokens;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

// slf4j logger imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class for manage EndpointsAccessTokens repository
 */
public class EndpointAccessTokensRepository implements AutoSavable {

    /**
     * {@link com.fasterxml.jackson.databind.ObjectMapper} Jackson mapper object for work with json.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * {@link com.serbekun.ss.core.EndpointsAccessTokens} object for
     *  load to him data from file
     *  and get data that will be saved.
     */
    private final EndpointsAccessTokens endpointsAccessTokens;

    /**
     * path to file where will be load and save data.
     */
    private final Path file;

    /**
     * slf4j logger for logging repository
     */
    private static final Logger log = 
        LoggerFactory.getLogger(EndpointAccessTokensRepository.class);

    /**
     * @param file path to file where will be load and save data.
     */
    public EndpointAccessTokensRepository(Path file) {
        this.file = file;
        this.endpointsAccessTokens = new EndpointsAccessTokens(load());
    }

    /**
     * 
     * Return {@link com.serbekun.ss.core.EndpointsAccessTokens} core class object 
     * need for get access to core object.
     * 
     * @return {@link com.serbekun.ss.core.EndpointsAccessTokens} core class object
     */
    public EndpointsAccessTokens getEndpointAccessTokens() {
        return endpointsAccessTokens;
    }

    /**
     * Load all EndpointsAccessTokens and index them by token string.
     * If the storage file is missing, an empty map is returned.
     */
    public Map<String, List<Endpoint>> load() {

        File f = file.toFile();

        // check file exit
        // if not exist just new hash map
        // without error log
        if (!f.exists()) {
            log.info("File not found start from new HashMap");
            return new LinkedHashMap<String, List<Endpoint>>();
        }

        try {
            // trying reading json value and convert to java object
            var result = mapper.readValue(
                    f,
                    new TypeReference<Map<String, List<Endpoint>>>() {}
            );

            // reading data successfully return data
            return result;

        } catch (IOException e) {
            // if reading error but file exist show error log
            log.info("Error load EndpointsAccessTokens: {}", e);
            log.info("starting from new hashmap");
            // and still return new hash map
            return new LinkedHashMap<String, List<Endpoint>>();
        }
    
    }

    /**
     * Save to json file value in memory
     */
    @Override
    public void save() {
        
        File f = file.toFile();

        try {
            mapper.writerWithDefaultPrettyPrinter()
                  .writeValue(f, endpointsAccessTokens.getAllTokens());
        
        } catch (IOException e) {
            log.error("Error save EndpointsAccessTokens: {}", e);
        }
    }
}
