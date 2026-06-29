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

    /** class that contain configuration for {@link com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo} */
    public static class LinksRepositoryConfig {
        private static final Path repositoriesStorageFile = Path.of("repository/repositories/links_repositories.json");

        public static Path getRepositoriesStorageFile() { return repositoriesStorageFile; }
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
        private static final Path uploadedFilesRAWFolder = Path.of("repository/uploaded_files_raw/");
        /**
         * Returns the path to the uploaded files storage file.
         * This file is used to store uploaded files data in JSON format.
         */
        private static final Path uploadedFilesStorageFile = Path.of("repository/uploaded_files/uploaded_files.json");

        /**
         * Returns the path to the folder where raw uploaded files are stored.
         * @return the path to the folder where raw uploaded files are stored
         */
        public static Path getUploadedFilesRAWFolder() { return uploadedFilesRAWFolder; }

        /**
         * Returns the path to the uploaded files storage file.
         * @return the path to the uploaded files storage file
         */
        public static Path getUploadedFilesStorageFile() { return uploadedFilesStorageFile; }
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
        private static final Path cookiesPath = Path.of("repository/www.youtube.com_cookies.txt");

        /**
         * Returns the path to the YouTube cookies file.
         * @return the path to the YouTube cookies file
         */
        public static Path getCookiesPath() { return cookiesPath; }
    }
}
