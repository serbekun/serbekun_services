package com.serbekun.ss.config;

import java.nio.file.Path;

/** class that contain paths to files that can be used in server */
public class Paths {
    
    /** class for contain configuration for {@link com.serbekun.ss.infrastructure} */
    public static class Infrastructure {

        /** class for contain configuration for {@link com.serbekun.ss.infrastructure.fs} */
        public static class Fs {
            private static final String serverStorageFolder = "repository";

            public static String getServerStorageFolder() { return serverStorageFolder; }
        }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.links.LinksRepo} */
    public static class LinksConfig {
        private static final Path linksStorageFile = Path.of("repository/catalogs/links.json");
        private static final Path linksLocalTokensStorageFile = Path.of("repository/tokens/links_local_tokens.json");

        public static Path getLinksStorageFile() { return linksStorageFile; }
        public static Path getLinksLocalTokensStorageFile() { return linksLocalTokensStorageFile; }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo} */
    public static class TokensConfig {
        private static final Path tokensStorageFolder = Path.of("repository/endpoint_access_tokens.json");

        public static Path getTokensStorageFolder() { return tokensStorageFolder; }

    }

    public static class UploadedFilesConfig {
        private static final Path UploadedFilesRAWFolder = Path.of("repository/uploaded_files_raw/");
        private static final Path UploadedFilesStorageFile = Path.of("repository/uploaded_files/uploaded_files.json");

        public static Path getUploadedFilesRAWFolder() { return UploadedFilesRAWFolder; }
        public static Path getUploadedFilesStorageFile() { return UploadedFilesStorageFile; }
    }

    public static class YoutubeConfig {
        private static final Path CookiesPath = Path.of("repository/www.youtube.com_cookies.txt");

        public static Path getCookiesPath() { return CookiesPath; }
    }
}
