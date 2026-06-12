package com.serbekun.ss.resources;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Checks if a resource exists on disk.
     *
     * @param path path to the resource
     * @return true if the file exists on disk, false otherwise
     */
    private boolean existsOnDisk(String path) {
        Path file = Path.of(path);
        return Files.exists(file) && Files.isRegularFile(file);
    }

    /**
     * Reads binary data of a resource from disk.
     *
     * @param path path to the resource
     * @return binary data read from disk, or null if not found or error
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

    /**
     * Gets an InputStream for a resource using the class loader.
     *
     * @param name resource name
     * @return InputStream or null if not found
     */
    private InputStream getResourceAsStream(String name) {
        // Use class loader of this class (important in JAR / module path)
        return ResourceLoader.class.getClassLoader().getResourceAsStream(name);
    }

    /**
     * Returns a list of all resources at the specified basePath (e.g. "html/", "images/", etc.)
     *
     * @param basePath path inside classpath ( must end with '/')
     * @return list of path to files (example: "html/index.html", "images/logo.png")
     */
    public List<String> listResources(String basePath) {
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        // in first check on disk
        List<String> fromDisk = listFromDisk(basePath);
        if (!fromDisk.isEmpty()) {
            return fromDisk;
        }

        // Searching in Jar / classpath
        return listFromClasspath(basePath);
    }

    /**
     * Lists all files in the specified directory on disk.
     *
     * @param basePath path to the directory
     * @return list of resource paths found on disk
     */
    private List<String> listFromDisk(String basePath) {
        java.nio.file.Path dir = java.nio.file.Path.of(basePath);
        if (!java.nio.file.Files.exists(dir) || !java.nio.file.Files.isDirectory(dir)) {
            return List.of();
        }

        try (var stream = java.nio.file.Files.walk(dir, 1)) { // Only 1 level
            return stream
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(p -> basePath + p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list files from disk: {}", basePath, e);
            return List.of();
        }
    }

    /**
     * Lists all resources from classpath (JAR or directory).
     *
     * @param basePath base path to search under
     * @return distinct sorted list of resource paths
     */
    private List<String> listFromClasspath(String basePath) {
        List<String> result = new ArrayList<>();

        try {
            Enumeration<URL> urls = ResourceLoader.class.getClassLoader().getResources(basePath);
            
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String protocol = url.getProtocol().toLowerCase();

                if ("jar".equals(protocol)) {
                    result.addAll(listFromJar(url, basePath));
                } else if ("file".equals(protocol)) {
                    java.nio.file.Path path = java.nio.file.Paths.get(url.toURI());
                    result.addAll(listFromDirectory(path, basePath));
                }
            }
        } catch (Exception e) {
            log.error("Failed to list resources from classpath: {}", basePath, e);
        }

        return result.stream().distinct().sorted().collect(Collectors.toList());
    }

    /**
     * Lists resources inside a JAR file under the given base path.
     *
     * @param jarUrl  URL to the JAR
     * @param basePath base directory path inside the JAR
     * @return list of matching resource names
     */
    private List<String> listFromJar(URL jarUrl, String basePath) {
        List<String> result = new ArrayList<>();
        String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));

        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.startsWith(basePath) && !entry.isDirectory()) {
                    result.add(name);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read JAR file", e);
        }

        return result;
    }

    /**
     * Lists files in a directory on the filesystem.
     *
     * @param dir      directory path
     * @param basePath base path prefix to use for results
     * @return list of resource paths
     */
    private List<String> listFromDirectory(java.nio.file.Path dir, String basePath) {
        try (var stream = java.nio.file.Files.walk(dir, 1)) {
            return stream
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(p -> basePath + p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list directory {}", dir, e);
            return List.of();
        }
    }
}
