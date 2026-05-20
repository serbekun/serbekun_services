package com.serbekun.ss.resources;

public final class ResourcesBasePath {

    private ResourcesBasePath() {}

    public static final String BASE_HTML_PATH = "html/";
    public static final String BASE_IMAGES_PATH = "images/";
    public static final String BASE_JSON_PATH = "json/";

    /**
     * Resolves the full path for an HTML resource.
     *
     * @param name the name of the HTML file
     * @return the full path
     */
    public static String resolveHtmlPath(String name) {
        return resolve(BASE_HTML_PATH, name) + ".html";
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

    public static String resolveJsonPath(String filename) {
        return resolve(BASE_JSON_PATH, filename) + ".json";
    }

    /**
     * 
     * Resolve path just base + name
     * 
     * @param base base folder of resource path
     * @param name filename that will be resolved
     * @return resolved path
     */
    public static String resolve(String base, String name) {
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + name;
    }
}