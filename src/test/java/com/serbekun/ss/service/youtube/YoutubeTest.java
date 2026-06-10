package com.serbekun.ss.service.youtube;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeTest {

    @Test
    void DownloadVideoTest() throws IOException {
        // This is an integration test that requires yt-dlp to be installed
        // and a working internet connection to YouTube
        
        String url = "https://www.youtube.com/watch?v=pZgCd6cZEHU";

        byte[] videoBytes = Youtube.DownloadVideoByUrl(url);

        assertThat(videoBytes).isNotNull();
        assertThat(videoBytes).isNotEmpty();

        Path outputDir = Path.of("test_tmp");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve("video.mp4");
        Files.write(outputFile, videoBytes);

        assertThat(outputFile).isNotEmptyFile();
    }

    @Test
    void DownloadVideoWithInvalidUrlTest() {
        // Should throw IllegalStateException for invalid URL
        String invalidUrl = "https://www.youtube.com/watch?v=invalid";
        
        assertThatThrownBy(() -> Youtube.DownloadVideoByUrl(invalidUrl))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("yt-dlp failed");
    }

    @Test
    void DownloadVideoWithNullUrlTest() throws IOException {
        // Should handle null URL gracefully
        assertThatThrownBy(() -> Youtube.DownloadVideoByUrl(null))
            .isInstanceOf(Exception.class);
    }
}
