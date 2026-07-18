package com.serbekun.ss.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void loadCreatesDefaultConfigWhenFileMissing() {
        Path file = tempDir.resolve("nested/config.json");

        Config config = Config.load(file);

        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getUploadFileMaxSizeMb()).isEqualTo(20);
        assertThat(config.getYoutubeProcessTimeoutSeconds()).isEqualTo(30);
        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void loadReadsValuesFromFile() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, """
            {
              "port": 9090,
              "upload_file_max_size_mb": 50,
              "youtube_process_timeout_seconds": 120,
              "yt_dlp_path": "/opt/yt-dlp",
              "deno_path": "/opt/deno"
            }
            """);

        Config config = Config.load(file);

        assertThat(config.getPort()).isEqualTo(9090);
        assertThat(config.getUploadFileMaxSizeMb()).isEqualTo(50);
        assertThat(config.getUploadFileMaxSizeBytes()).isEqualTo(50L * 1024 * 1024);
        assertThat(config.getYoutubeProcessTimeoutSeconds()).isEqualTo(120);
        assertThat(config.getYtDlpPath()).isEqualTo("/opt/yt-dlp");
        assertThat(config.getDenoPath()).isEqualTo("/opt/deno");
    }

    @Test
    void loadFallsBackToDefaultsOnCorruptFile() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, "{ this is not json");

        Config config = Config.load(file);

        assertThat(config.getPort()).isEqualTo(8080);
        assertThat(config.getUploadFileMaxSizeMb()).isEqualTo(20);
    }

    @Test
    void legacyByteSizeIsConvertedToMegabytes() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file, "{\"port\": 8080, \"upload_file_max_size\": 10485760}");

        Config config = Config.load(file);

        assertThat(config.getUploadFileMaxSizeMb()).isEqualTo(10);
    }

    @Test
    void legacyByteSizeIsRoundedUp() throws IOException {
        Path file = tempDir.resolve("config.json");
        // 1 MB + 1 byte must round up to 2 MB
        Files.writeString(file, "{\"port\": 8080, \"upload_file_max_size\": 1048577}");

        Config config = Config.load(file);

        assertThat(config.getUploadFileMaxSizeMb()).isEqualTo(2);
    }

    @Test
    void megabyteFieldTakesPrecedenceOverLegacyField() throws IOException {
        Path file = tempDir.resolve("config.json");
        Files.writeString(file,
            "{\"port\": 8080, \"upload_file_max_size_mb\": 5, \"upload_file_max_size\": 10485760}");

        Config config = Config.load(file);

        assertThat(config.getUploadFileMaxSizeMb()).isEqualTo(5);
    }
}
