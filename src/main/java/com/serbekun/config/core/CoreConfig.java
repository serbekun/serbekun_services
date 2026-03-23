package com.serbekun.config.core;

import java.nio.file.Path;

/**
 * class that contain core config
 */
public class CoreConfig {
    
    /**
     * class for contain configuration for {@link com.serbekun.infrastructure}
     */
    public static class Infrastructure {

        /**
         * class for contain configuration for {@link com.serbekun.infrastructure.fs}
         */
        public static class Fs {
            private static final String serverStorageFolder = "repository";

            public static String getServerStorageFolder() { return serverStorageFolder; }
        }
    }

    /**
     * class that contain configuration for {@link com.serbekun.core.Links}
     */
    public static class LinksConfig {
        private static final Path linksStorageFile = Path.of("repository/links.json");
        private static final Path linksLocalTokensStorageFile = Path.of("repository/links_local_tokens.json");

        public static Path getLinksStorageFile() { return linksStorageFile; }
        public static Path getLinksLocalTokensStorageFile() { return linksLocalTokensStorageFile; }
    }

    /**
     * class that contain configuration for {}
     */
    public static class ProgramsConfig {
        private static final Path programsStorageFolder = Path.of("repository/programs/");

        public static Path getProgramsStorageFolder() { return programsStorageFolder; }
    }

    /**
     * class that contain configuration for {@link com.serbekun.core.EndpointsAccessTokens}
     */
    public static class TokensConfig {
        private static final Path tokensStorageFolder = Path.of("repository/endpoint_access_tokens.json");

        public static Path getTokensStorageFolder() { return tokensStorageFolder; }

    }
}
