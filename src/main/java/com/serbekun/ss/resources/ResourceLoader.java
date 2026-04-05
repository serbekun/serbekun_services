package com.serbekun.ss.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Responsible for loading resources from disk first, then classpath/JAR.
 */
public class ResourceLoader {
    private static final Logger log = LoggerFactory.getLogger(ResourceLoader.class);

    /**
     * Loads a resource as binary data from disk first, then classpath.
     *
     * @param path the path to the resource
     * @return byte array or null if not found or error
     */
    public byte[] loadBinary(String path) {
        // check exist resource in disk
        byte[] diskBytes = loadFromDisk(path);
        if (diskBytes != null) {
            return diskBytes;
        }
        // if resource don't exist in disk try to read from jar file
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
        // check in disk
        if (existsOnDisk(path)) {
            return true;
        }
        // check in jar file
        try (InputStream is = getResourceAsStream(path)) {
            return is != null;
        } catch (IOException e) {
            log.error("Error checking if resource exists: {}", path, e);
            return false;
        }
    }

    /**
     * 
     * Help function for check exist resource on disk.
     * 
     * @param path path to resource
     * @return true file exist on disk. false don't exist.
     */
    private boolean existsOnDisk(String path) {
        Path file = Path.of(path);
        return Files.exists(file) && Files.isRegularFile(file);
    }

    /**
     * 
     * Read resource binary data from disk by path
     * 
     * @param path path to resource
     * @return reded from disk resource binary data.
     */
    private byte[] loadFromDisk(String path) {
        Path file = Path.of(path);
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.error("Failed to read resource from disk: {}", file, e);
            return null;
        }
    }

    private InputStream getResourceAsStream(String name) {
        // Use class loader of this class (important in JAR / module path)
        return ResourceLoader.class.getClassLoader().getResourceAsStream(name);
    }
}
