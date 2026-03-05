package com.serbekun.config.core;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.stdDSA;

/**
 * class that contain core config
 */
public class CoreConfig {
    
    public static class Infrastructure {

        public static class Fs {
            private static final String serverStorageFolder = "repository";

            public static String getServerStorageFolder() {
                return serverStorageFolder;
            }
        }

    }

    /**
     * class that contain configuration for {@link core.Links}
     */
    public static class LinksConfig {
        private static final String linksStorageFile = "repository/links.json";

        public static String getLinksStorageFile() { return linksStorageFile; }
    }
}
