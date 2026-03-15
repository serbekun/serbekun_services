package com.serbekun.config.core;

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
        private static final String linksStorageFile = "repository/links.json";

        public static String getLinksStorageFile() { return linksStorageFile; }
    }

    /**
     * class that contain configuration for {}
     */
    public static class ProgramsConfig {
        private static final String programsStorageFolder = "repository/programs/";

        public static String getProgramsStorageFolder() { return programsStorageFolder; }
    }

    /**
     * class that contain configuration for {@link com.serbekun.core.Tokens}
     */
    public static class TokensConfig {
        private static final String tokensStorageFolder = "repository/tokens.json";

        public static String getTokensStorageFolder() { return tokensStorageFolder; }

    }
}
