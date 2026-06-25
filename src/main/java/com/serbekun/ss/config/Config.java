package com.serbekun.ss.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * COnfig class load config of the from file and create config 
 * file with defaults value if config file not found.
 */
public class Config {

    // region Fields

    // region Dependencies
    /** {@link org.slf4j.LoggerFactory} object for logging. */
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    /** {@link com.fasterxml.jackson.databind.ObjectMapper} for serialize and deserialize json. */
    private static final ObjectMapper mapper = new ObjectMapper();
    // endregion

    // region Config value
    
    /** Server port value */
    private final int port;
    /** Server max upload file size value */
    private final int uploadFileMaxSize;

    /** How many time server can download videos from youtube */
    private final long YoutubeProcessTimeoutSeconds;
    /** Path to yt-dlp */
    private final String YtDlpPath;
    /** Path to deno */
    private final String DenoPath;

    // endregion
    // endregion 

    // region Methods

    @JsonCreator
    public Config(@JsonProperty("port") int port,
    @JsonProperty("upload_file_max_size") int uploadFileMaxSize,
    @JsonProperty("youtube_process_timeout_seconds") long youtubeProcessTimeoutSeconds,
    @JsonProperty("yt_dlp_path") String ytDlpPath,
    @JsonProperty("deno_path") String denoPath
) {
        this.port = port;
        this.uploadFileMaxSize = uploadFileMaxSize;
        this.YoutubeProcessTimeoutSeconds = youtubeProcessTimeoutSeconds;
        this.YtDlpPath = ytDlpPath;
        this.DenoPath = denoPath;
    }

    // region Getters

    /** @return Server http bind port */
    @JsonProperty("port")
    public int getPort() { return port; }

    /** @return Server upload file max size */
    @JsonProperty("upload_file_max_size")
    public int getUploadFileMaxSize() { return uploadFileMaxSize; }

    /** @return Youtube process timeout in seconds */
    @JsonProperty("youtube_process_timeout_seconds")
    public long getYoutubeProcessTimeoutSeconds() { return YoutubeProcessTimeoutSeconds; }

    /** @return Path to yt-dlp */
    @JsonProperty("yt_dlp_path")
    public String getYtDlpPath() { return YtDlpPath; }

    /** @return Path to deno */
    @JsonProperty("deno_path")
    public String getDenoPath() { return DenoPath; }

    // endregion
    
    // region FS
    
    /**
     * 
     * Load {@link Config} object from file
     * 
     * <p> Load file contents by path in given params after
     * deserialize json to {@link Config} Object </p>
     * 
     * @param file
     * @return {@link Config} object 
    */
    public static Config load(Path file) {
       File f = file.toFile();
       
       // check is file exist
       // if file not exist
       // create Config with defaults values
       // using defaultConfig();
       // and save to file
       if (!f.exists()) {
           log.info("Config file not found, creating default config at {}", file);
           Config defaults = defaultConfig();
           save(defaults, f);
           return defaults;
        }
        
        // try to deserialize json from file
        // if deserializing was not successfully
        // create Config with defaults values
        // and save to file
        try {
            return mapper.readValue(f, Config.class);
        } catch (IOException e) {
            log.error("Error reading config file: {}", e.getMessage());
            log.info("Falling back to default config");
            return defaultConfig();
        }
    }
    
    /**
     * 
     * Save config 
     * 
     * @param config {@link Config}
     * @param file
     */
    private static void save(Config config, File file) {
        // try serialize Config object to json and save to file
        // if doing this was not successfully just show error log message
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
    
    // endregion

    // region Helpers

    /** @return Config object with default values. */
    private static Config defaultConfig() {
        return new Config(8080, 20971520, 30, "/usr/local/bin/yt-dlp", "/usr/local/bin/deno");
    }

    // endregion
    // endregion

}
