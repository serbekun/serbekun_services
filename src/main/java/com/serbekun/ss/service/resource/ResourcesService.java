package com.serbekun.ss.service.resource;

import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.resources.ResourcesBasePath;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Public API for loading static resources.
 * Combines resource loading and caching functionality.
 */
public class ResourcesService {
    
    private static final Logger log = LoggerFactory.getLogger(ResourcesService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ResourceCache cache;

    /**
     * Creates a new ResourcesService with the given ResourceLoader and ResourceCache.
     *
     * @param loader the resource loader (unused, kept for backward compatibility)
     * @param cache the resource cache
     */
    public ResourcesService(ResourceLoader loader, ResourceCache cache) {
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

    // region Static resource helpers

    /**
     * Returns the binary data for an image resource.
     * @param name the name of the image resource
     * @return the binary data for the image resource
     */
    public byte[] getImage(String name) {
        if (name == null) name = "";
        if (name.isEmpty()) {
            return null; // listing is handled separately
        }
        String path = ResourcesBasePath.resolveImagePath(name);
        return getBinaryData(path);
    }

    /**
     * Returns the JSON data for a resource.
     * @param name the name of the JSON resource
     * @return the JSON data for the resource
     */
    public String getJson(String name) {
        return getTextResourceWithListing(name, 
            ResourcesBasePath.BASE_JSON_PATH,
            ResourcesBasePath::resolveJsonPath);
    }

    /**
     * Returns the HTML content for a given resource name.
     * If the name is empty, it returns a JSON list of available HTML files.
     * @param name the name of the HTML resource
     * @return the HTML content for the resource
     */
    public String getHtml(String name) {
        return getTextResourceWithListing(name,
            ResourcesBasePath.BASE_HTML_PATH,
            ResourcesBasePath::resolveHtmlPath);
    }

    /**
     * Returns the CSS content for a given resource name.
     * If the name is empty, it returns a JSON list of available CSS files.
     * @param name the name of the CSS resource
     * @return the CSS content for the resource
     */
    public String getCss(String name) {
        return getTextResourceWithListing(name,
            ResourcesBasePath.BASE_CSS_PATH,
            ResourcesBasePath::resolveCssPath);
    }

    /**
     * Returns the JavaScript content for a given resource name.
     * If the name is empty, it returns a JSON list of available JavaScript files.
     * @param name the name of the JavaScript resource
     * @return the JavaScript content for the resource
     */
    public String getJs(String name) {
        return getTextResourceWithListing(name,
            ResourcesBasePath.BASE_JS_PATH,
            ResourcesBasePath::resolveJsPath);
    }

    /**
     * Returns the binary data for a PDF resource.
     * @param name the name of the PDF resource
     * @return the binary data for the resource
     */
    public byte[] getPdf(String name) {
        if (name == null) name = "";
        if (name.isEmpty()) {
            return null;
        }
        String path = ResourcesBasePath.resolvePdfPath(name);
        return getBinaryData(path);
    }

    /**
     * Returns a JSON list of available PDF files in the PDFs directory.
     * @return the JSON string containing the list of PDF files
     */
    public String listPdfsAsJson() {
        return listResourcesAsJson(ResourcesBasePath.BASE_PDF_PATH);
    }

    /**
     * Returns a JSON list of available image files in the images directory.
     * @return the JSON string containing the list of image files
     */
    public String listImagesAsJson() {
        return listResourcesAsJson(ResourcesBasePath.BASE_IMAGES_PATH);
    }

    public String getDomain(String name) {
        return getTextResourceWithListing(name,
            ResourcesBasePath.BASE_DOMAIN_PATH,
            ResourcesBasePath::resolveDomainPath);
    }

    public String listDomainsAsJson() {
        return listResourcesAsJson(ResourcesBasePath.BASE_DOMAIN_PATH);
    }

    /**
     * Returns the text content for a named resource, or a JSON listing of the
     * files available under {@code basePath} when {@code name} is null or empty.
     *
     * @param name     the resource name; null/empty returns the directory listing
     * @param basePath the base directory used for listing
     * @param resolver maps a resource name to its full resource path
     * @return the resource text, the JSON listing, or {@code null} on error
     */
    private String getTextResourceWithListing(String name, String basePath, Function<String, String> resolver) {
        if (name == null || name.isEmpty()) {
            return listResourcesAsJson(basePath);
        }
        return getTextData(resolver.apply(name));
    }

    /**
     * Returns a JSON array of the resource file names under {@code basePath},
     * with the base path stripped from each entry.
     *
     * @param basePath the directory to list
     * @return the JSON listing, or {@code null} if serialization fails
     */
    private String listResourcesAsJson(String basePath) {
        List<String> filesList = listResources(basePath).stream()
            .map(file -> file.startsWith(basePath) ? file.substring(basePath.length()) : file)
            .toList();
        try {
            return OBJECT_MAPPER.writeValueAsString(filesList);
        } catch (Exception e) {
            log.warn("Failed to serialize resource listing for {}", basePath, e);
            return null;
        }
    }

    /**
     * A map of file extensions to their corresponding MIME types.
     */
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
