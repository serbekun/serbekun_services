package com.serbekun.ss.config;

import java.nio.file.Path;

/** class that contain paths to files that can be used in server */
public class Paths {
    
    /** class for contain configuration for {@link com.serbekun.ss.infrastructure} */
    public static class Infrastructure {

        /** class for contain configuration for {@link com.serbekun.ss.infrastructure.fs} */
        public static class Fs {
            /** Returns the path to the server storage folder. */
            private static final String serverStorageFolder = "repository";
            
            /**
             * Returns the path to the server storage folder.
             * @return the path to the server storage folder
             */
            public static String getServerStorageFolder() { return serverStorageFolder; }
        }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.links.LinksRepo} */
    public static class LinksConfig {
        /** Returns the path to the links storage file. 
         * This file is used to store links data in JSON format.
         */
        private static final Path linksStorageFile = Path.of("repository/catalogs/links.json");

        /**
         * Returns the path to the links local tokens storage file.
         * This file is used to store local tokens for links.
         * @return the path to the links local tokens storage file
         */
        private static final Path linksLocalTokensStorageFile = Path.of("repository/tokens/links_local_tokens.json");

        /**
         * Returns the path to the links storage file.
         * @return the path to the links storage file
         */
        public static Path getLinksStorageFile() { return linksStorageFile; }

        /**
         * Returns the path to the links local tokens storage file.
         * @return the path to the links local tokens storage file
         */
        public static Path getLinksLocalTokensStorageFile() { return linksLocalTokensStorageFile; }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo} */
    public static class TokensConfig {
        private static final Path tokensStorageFolder = Path.of("repository/endpoint_access_tokens.json");

        /**
         * Returns the path to the tokens storage folder.
         * @return the path to the tokens storage folder
         */
        public static Path getTokensStorageFolder() { return tokensStorageFolder; }

    }

    public static class UploadedFilesConfig {
        /** Returns the path to the folder where raw uploaded files are stored. */
        private static final Path UploadedFilesRAWFolder = Path.of("repository/uploaded_files_raw/");
        /**
         * Returns the path to the uploaded files storage file.
         * This file is used to store uploaded files data in JSON format.
         */
        private static final Path UploadedFilesStorageFile = Path.of("repository/uploaded_files/uploaded_files.json");

        /**
         * Returns the path to the folder where raw uploaded files are stored.
         * @return the path to the folder where raw uploaded files are stored
         */
        public static Path getUploadedFilesRAWFolder() { return UploadedFilesRAWFolder; }

        /**
         * Returns the path to the uploaded files storage file.
         * @return the path to the uploaded files storage file
         */
        public static Path getUploadedFilesStorageFile() { return UploadedFilesStorageFile; }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.shorturl.ShortUrlRepo} */
    public static class ShortUrlConfig {
        /**
         * Returns the path to the short url storage file.
         * This file is used to store short url records in JSON format.
         */
        private static final Path shortUrlStorageFile = Path.of("repository/short_url/short_url.json");

        /**
         * Returns the path to the short url storage file.
         * @return the path to the short url storage file
         */
        public static Path getShortUrlStorageFile() { return shortUrlStorageFile; }
    }

    public static class YoutubeConfig {
        /** Returns the path to the YouTube cookies file. */
        private static final Path CookiesPath = Path.of("repository/www.youtube.com_cookies.txt");

        /**
         * Returns the path to the YouTube cookies file.
         * @return the path to the YouTube cookies file
         */
        public static Path getCookiesPath() { return CookiesPath; }
    }
}
