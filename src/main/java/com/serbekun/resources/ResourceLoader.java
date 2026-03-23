package com.serbekun.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Responsible for loading resources from the classpath/JAR.
 */
public class ResourceLoader {
    private static final Logger log = LoggerFactory.getLogger(ResourceLoader.class);

    /**
     * Loads a resource as binary data from the classpath.
     *
     * @param path the path to the resource
     * @return byte array or null if not found or error
     */
    public byte[] loadBinary(String path) {
        try (InputStream is = getResourceAsStream(path)) {
            if (is == null) {
                log.warn("Resource not found: {}", path);
                return null;
            }
            return is.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to load binary resource: {}", path, e);
            return null;
        }
    }

    /**
     * Loads a resource as text from the classpath.
     *
     * @param path the path to the resource
     * @param charset the charset to use for decoding
     * @return string or null if not found or error
     */
    public String loadText(String path, Charset charset) {
        byte[] bytes = loadBinary(path);
        if (bytes == null) {
            return null;
        }
        return new String(bytes, charset);
    }

    /**
     * Checks if a resource exists on the classpath.
     *
     * @param path the path to the resource
     * @return true if the resource exists, false otherwise
     */
    public boolean exists(String path) {
        try (InputStream is = getResourceAsStream(path)) {
            return is != null;
        } catch (IOException e) {
            log.error("Error checking if resource exists: {}", path, e);
            return false;
        }
    }

    private InputStream getResourceAsStream(String name) {
        // Use class loader of this class (important in JAR / module path)
        return ResourceLoader.class.getClassLoader().getResourceAsStream(name);
    }
}