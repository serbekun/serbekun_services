package com.serbekun.ss.resources;

public final class ResourcesBasePath {

    private ResourcesBasePath() {}

    public static final String BASE_HTML_PATH = "html/";
    public static final String BASE_CSS_PATH = "css/";
    public static final String BASE_JS_PATH = "js/";

    public static final String BASE_IMAGES_PATH = "images/";
    public static final String BASE_JSON_PATH = "json/";
    public static final String BASE_PDF_PATH = "pdf/";
    public static final String BASE_DOMAIN_PATH = "domain/";

    /**
     * Resolves the full path for an HTML resource.
     *
     * @param name the name of the HTML file
     * @return the full path
     */
    public static String resolveHtmlPath(String name) {
        return resolve(BASE_HTML_PATH, name);
    }

    public static String resolveCssPath(String filename) {
        return resolve(BASE_CSS_PATH, filename);
    }

    public static String resolveJsPath(String filename) {
        return resolve(BASE_JS_PATH, filename);
    }
    
    /**
     * Resolves the full path for an image resource.
     *
     * @param filename the image filename
     * @return the full path
     */
    public static String resolveImagePath(String filename) {
        return resolve(BASE_IMAGES_PATH, filename);
    }

    public static String resolvePdfPath(String filename) {
        return resolve(BASE_PDF_PATH, filename);
    }

    public static String resolveDomainPath(String filename) {
        return resolve(BASE_DOMAIN_PATH, filename);
    }

    public static String resolveJsonPath(String filename) {
        return resolve(BASE_JSON_PATH, filename);
    }


    /**
     *
     * Resolve path just base + name.
     * Rejects names containing path traversal sequences.
     *
     * @param base base folder of resource path
     * @param name filename that will be resolved
     * @return resolved path
     * @throws IllegalArgumentException if name contains path traversal characters
     */
    public static String resolve(String base, String name) {
        if (name == null) {
            name = "";
        }
        if (name.contains("..") || name.contains("/") || name.contains("\\") || name.contains("%")) {
            throw new IllegalArgumentException("Invalid resource name: path traversal not allowed");
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + name;
    }
}