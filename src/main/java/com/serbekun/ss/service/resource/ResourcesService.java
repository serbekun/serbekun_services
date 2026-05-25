package com.serbekun.ss.service.resource;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Public API for loading static resources.
 * Combines resource loading and caching functionality.
 */
public class ResourcesService {
    // private static final Logger log = LoggerFactory.getLogger(ResourcesService.class);

    // private final ResourceLoader loader;
    private final ResourceCache cache;

    /**
     * Creates a new ResourcesService with the given ResourceLoader and ResourceCache.
     *
     * @param loader the resource loader
     * @param cache the resource cache
     */
    public ResourcesService(ResourceLoader loader, ResourceCache cache) {
        // this.loader = loader;
        this.cache = cache;
    }

    /** Returns text data for an arbitrary resource path using UTF-8 by default. */
    public String getTextData(String path) {
        return getTextData(path, StandardCharsets.UTF_8);
    }

    /** Returns text data for an arbitrary resource path using the provided charset. */
    public String getTextData(String path, Charset charset) {
        return cache.getText(path, charset);
    }

    /** Returns binary data for an arbitrary resource path. */
    public byte[] getBinaryData(String path) {
        return cache.getBinary(path);
    }

    /**
     * Detects MIME type based on file extension.
     *
     * @param filename the filename
     * @return MIME type string or "application/octet-stream" if unknown
     */
    public String detectMimeType(String filename) {
        String extension = getFileExtension(filename);
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * Clears all cached resources.
     */
    public void clearCache() {
        cache.clear();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase();
    }

    /**
     * 
     * Return list of available files in directory
     * 
     * @param basePath path to folder
     * @return list of available files in directory
     */
    public List<String> listResources(String basePath) {
        return cache.listResources(basePath);
    }

    // === Static resource helpers (moved from deprecated service/http layer) ===

    public byte[] getImage(String name) {
        if (name == null) name = "";
        if (name.isEmpty()) {
            return null; // listing is handled separately
        }
        String path = com.serbekun.ss.resources.ResourcesBasePath.resolveImagePath(name);
        return getBinaryData(path);
    }

    public String getJson(String name) {
        if (name == null) name = "";
        if (name.isEmpty()) {
            String path = com.serbekun.ss.resources.ResourcesBasePath.BASE_JSON_PATH;
            List<String> filesList = listResources(path).stream()
                .map(file -> file.startsWith(path) ? file.substring(path.length()) : file)
                .toList();
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(filesList);
            } catch (Exception e) {
                return null;
            }
        }
        String path = com.serbekun.ss.resources.ResourcesBasePath.resolveJsonPath(name);
        return getTextData(path);
    }

    public String getHtml(String name) {
        if (name == null) name = "";
        if (name.isEmpty()) {
            String path = com.serbekun.ss.resources.ResourcesBasePath.BASE_HTML_PATH;
            List<String> filesList = listResources(path).stream()
                .map(file -> file.startsWith(path) ? file.substring(path.length()) : file)
                .toList();
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(filesList);
            } catch (Exception e) {
                return null;
            }
        }
        String path = com.serbekun.ss.resources.ResourcesBasePath.resolveHtmlPath(name);
        return getTextData(path);
    }

    public String listImagesAsJson() {
        String path = com.serbekun.ss.resources.ResourcesBasePath.BASE_IMAGES_PATH;
        List<String> filesList = listResources(path).stream()
            .map(file -> file.startsWith(path) ? file.substring(path.length()) : file)
            .toList();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(filesList);
        } catch (Exception e) {
            return null;
        }
    }

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
        Map.entry("html", "text/html"),
        Map.entry("htm", "text/html"),
        Map.entry("css", "text/css"),
        Map.entry("js", "application/javascript"),
        Map.entry("json", "application/json"),
        Map.entry("xml", "application/xml"),
        Map.entry("txt", "text/plain"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("png", "image/png"),
        Map.entry("gif", "image/gif"),
        Map.entry("bmp", "image/bmp"),
        Map.entry("webp", "image/webp"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("ico", "image/x-icon"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("zip", "application/zip")
    );
}
