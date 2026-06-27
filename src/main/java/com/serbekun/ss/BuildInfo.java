package com.serbekun.ss;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides information about the build.
 */
public final class BuildInfo {

    private static final String VERSION;

    static {
        Properties props = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/com/serbekun/ss/version.properties")) {
            if (in == null) {
                throw new IllegalStateException("version.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load version.properties", e);
        }
        VERSION = props.getProperty("version", "unknown");
    }

    private BuildInfo() {
    }

    public static String version() {
        return VERSION;
    }
}
