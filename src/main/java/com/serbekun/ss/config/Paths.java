package com.serbekun.ss.config;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Paths that can be used across the server. All values are resolved from
 * {@link Config} via {@link #init(Config)} at startup. Until init is called,
 * sane hardcoded defaults are used so that static initializers do not NPE.
 */
public class Paths {

    private static Config config;

    /** Set once at startup before any paths are used. */
    public static void init(Config c) {
        config = c;
    }

    private static String configOrDefault(Function<Config, String> getter, String fallback) {
        if (config == null) return fallback;
        String value = getter.apply(config);
        return value != null ? value : fallback;
    }

    /** class for contain configuration for {@link com.serbekun.ss.infrastructure} */
    public static class Infrastructure {

        /** class for contain configuration for {@link com.serbekun.ss.infrastructure.fs} */
        public static class Fs {
            /**
             * Returns the path to the server storage folder.
             * @return the path to the server storage folder
             */
            public static String getServerStorageFolder() {
                return configOrDefault(Config::getRepositoryFolder, "repository");
            }
        }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo} */
    public static class LinksRepositoryConfig {
        public static Path getRepositoriesStorageFile() {
            return Path.of(configOrDefault(Config::getLinksRepositoriesStorageFile,
                    "repository/repositories/links_repositories.json"));
        }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo} */
    public static class TokensConfig {
        /**
         * Returns the path to the tokens storage file.
         * @return the path to the tokens storage file
         */
        public static Path getTokensStorageFolder() {
            return Path.of(configOrDefault(Config::getTokensStorageFile,
                    "repository/endpoint_access_tokens.json"));
        }
    }

    public static class UploadedFilesConfig {
        /**
         * Returns the path to the folder where raw uploaded files are stored.
         * @return the path to the folder where raw uploaded files are stored
         */
        public static Path getUploadedFilesRAWFolder() {
            return Path.of(configOrDefault(Config::getUploadedFilesRawFolder,
                    "repository/uploaded_files_raw/"));
        }

        /**
         * Returns the path to the uploaded files storage file.
         * @return the path to the uploaded files storage file
         */
        public static Path getUploadedFilesStorageFile() {
            return Path.of(configOrDefault(Config::getUploadedFilesStorageFile,
                    "repository/uploaded_files/uploaded_files.json"));
        }
    }

    /** class that contain configuration for {@link com.serbekun.ss.repo.shorturl.ShortUrlRepo} */
    public static class ShortUrlConfig {
        /**
         * Returns the path to the short url storage file.
         * @return the path to the short url storage file
         */
        public static Path getShortUrlStorageFile() {
            return Path.of(configOrDefault(Config::getShortUrlStorageFile,
                    "repository/short_url/short_url.json"));
        }
    }

    public static class YoutubeConfig {
        /**
         * Returns the path to the YouTube cookies file.
         * @return the path to the YouTube cookies file
         */
        public static Path getCookiesPath() {
            return Path.of(configOrDefault(Config::getYoutubeCookiesFile,
                    "repository/www.youtube.com_cookies.txt"));
        }
    }
}
