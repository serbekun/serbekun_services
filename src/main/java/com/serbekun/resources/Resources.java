package com.serbekun.resources;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * class for get resources from jar file
 */
public class Resources {
    
    private static final Map<String, String> HTML_CACHE = new HashMap<>();

    /**
     * 
     * load binary from file
     * 
     * @param filename filename that load binary
     * @return
     */
    private static byte[] loadBinary(String filename) {
        try (InputStream is =
                Resources.class
                    .getClassLoader()
                    .getResourceAsStream(filename)) {

            if (is == null) {
                throw new RuntimeException(filename + " NOT FOUND");
            }

            return is.readAllBytes();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + filename, e);
        }
    }

    /** resources in jar file */
    static {
        load("index");
        load("chat");
        load("create_user");
        load("verification_text");
    }

    /**
     * load files to jar
     */
    private static void load(String name) {
        try (InputStream is = Resources.class
                .getClassLoader()
                .getResourceAsStream("html/" + name + ".html")) {

            if (is != null) {
                HTML_CACHE.put(name,
                    new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) {}
    }

    public static String getHtml(String name) {
        return HTML_CACHE.get(name);
    }

}
