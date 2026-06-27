package com.serbekun.ss.service.youtube;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YoutubeTest {

    private final Youtube youtube = new Youtube(120, "/home/sergei/.local/bin/yt-dlp", "/home/sergei/.deno/bin");

    // Test domains: a minimal set representing the YouTube domain list
    private static final Set<String> TEST_SUFFIX_DOMAINS = Set.of(
        "youtube.com",
        "youtu.be",
        "googlevideo.com",
        "ytimg.com"
    );
    private static final Set<String> TEST_EXACT_DOMAINS = Set.of(
        "yt3.googleusercontent.com"
    );
    private final YoutubeDomains testDomains = new YoutubeDomains(TEST_SUFFIX_DOMAINS, TEST_EXACT_DOMAINS);

    // ============ Integration tests (require yt-dlp) ============

    @Test
    void DownloadVideoTest() throws IOException {
        // This is an integration test that requires yt-dlp to be installed
        // and a working internet connection to YouTube

        String url = "https://www.youtube.com/watch?v=pZgCd6cZEHU";

        byte[] videoBytes = youtube.DownloadVideoByUrl(url);

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

        assertThatThrownBy(() -> youtube.DownloadVideoByUrl(invalidUrl))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("yt-dlp failed");
    }

    @Test
    void DownloadVideoWithNullUrlTest() throws IOException {
        // Should handle null URL gracefully
        assertThatThrownBy(() -> youtube.DownloadVideoByUrl(null))
            .isInstanceOf(Exception.class);
    }

    // ============ YoutubeDomains unit tests ============

    @Test
    void domainMatchesYouTubeSubdomain() {
        // www.youtube.com is a subdomain of youtube.com
        assertThat(testDomains.isHostAllowed("www.youtube.com")).isTrue();
        assertThat(testDomains.isHostAllowed("m.youtube.com")).isTrue();
        assertThat(testDomains.isHostAllowed("youtube.com")).isTrue();
    }

    @Test
    void domainMatchesShortUrl() {
        // youtu.be is a suffix domain
        assertThat(testDomains.isHostAllowed("youtu.be")).isTrue();
        assertThat(testDomains.isHostAllowed("www.youtu.be")).isTrue();
    }

    @Test
    void domainMatchesExactFullDomain() {
        // full: prefix means exact match only
        assertThat(testDomains.isHostAllowed("yt3.googleusercontent.com")).isTrue();
        // Subdomain of an exact domain should NOT match
        assertThat(testDomains.isHostAllowed("sub.yt3.googleusercontent.com")).isFalse();
    }

    @Test
    void domainRejectsNonYouTubeUrl() {
        assertThat(testDomains.isHostAllowed("evil.com")).isFalse();
        assertThat(testDomains.isHostAllowed("youtube.evil.com")).isFalse();
        assertThat(testDomains.isHostAllowed("google.com")).isFalse();
        assertThat(testDomains.isHostAllowed("vimeo.com")).isFalse();
    }

    @Test
    void domainRejectsSimilarButDifferentDomain() {
        // Not actually youtube.com
        assertThat(testDomains.isHostAllowed("youtube.com.phishing.com")).isFalse();
        assertThat(testDomains.isHostAllowed("fake-youtube.com")).isFalse();
    }

    @Test
    void isAllowedWithFullUrl() {
        assertThat(testDomains.isAllowed("https://www.youtube.com/watch?v=abc123")).isTrue();
        assertThat(testDomains.isAllowed("https://youtu.be/abc123")).isTrue();
        assertThat(testDomains.isAllowed("https://evil.com/watch?v=abc123")).isFalse();
    }

    @Test
    void extractHostFromValidUrls() {
        assertThat(YoutubeDomains.extractHost("https://www.youtube.com/watch?v=abc")).isEqualTo("www.youtube.com");
        assertThat(YoutubeDomains.extractHost("https://youtu.be/abc")).isEqualTo("youtu.be");
        assertThat(YoutubeDomains.extractHost("http://youtube.com")).isEqualTo("youtube.com");
    }

    @Test
    void extractHostFromInvalidUrlThrowsOnIsAllowed() {
        // extractHost returns null for unparseable input
        assertThat(YoutubeDomains.extractHost("")).isNull();
        assertThat(YoutubeDomains.extractHost(null)).isNull();
        // isAllowed should throw IllegalArgumentException when host cannot be extracted
        assertThatThrownBy(() -> testDomains.isAllowed("not-a-valid-url://"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Could not extract host");
    }

    // ============ YoutubeService domain filter tests ============

    @Test
    void serviceRejectsNonYouTubeDomain() {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        String nonYoutubeUrl = "https://vimeo.com/12345";
        assertThatThrownBy(() -> service.getVideoInfo(nonYoutubeUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not an allowed YouTube domain");
    }

    @Test
    void serviceRejectsYouTubeTypoDomain() {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        String typoUrl = "https://youtub.com/watch?v=abc";
        assertThatThrownBy(() -> service.getVideoInfo(typoUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not an allowed YouTube domain");
    }

    @Test
    void serviceRejectsPhishingDomain() {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        String phishingUrl = "https://youtube.com.evil.com/watch?v=abc";
        assertThatThrownBy(() -> service.getVideoInfo(phishingUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not an allowed YouTube domain");
    }

    @Test
    void serviceRejectsRandomSite() {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        String randomUrl = "https://www.google.com/search?q=youtube";
        assertThatThrownBy(() -> service.getVideoInfo(randomUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not an allowed YouTube domain");
    }

    @Test
    void serviceRejectsDomainInDownloadToo() {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        String nonYoutubeUrl = "https://dailymotion.com/video/abc";
        assertThatThrownBy(() -> service.downloadVideo(nonYoutubeUrl))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not an allowed YouTube domain");
    }

    @Test
    void serviceAcceptsValidYouTubeDomain() throws IOException {
        Youtube mockYoutube = mock(Youtube.class);
        when(mockYoutube.GetVideoInfo("https://www.youtube.com/watch?v=abc123"))
            .thenReturn("{\"title\":\"Test Video\"}");
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        String result = service.getVideoInfo("https://www.youtube.com/watch?v=abc123");
        assertThat(result).isEqualTo("{\"title\":\"Test Video\"}");
        verify(mockYoutube).GetVideoInfo("https://www.youtube.com/watch?v=abc123");
    }

    @Test
    void serviceDoesNotCallYtDlpWhenDomainInvalid() throws IOException {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        try {
            service.getVideoInfo("https://evil.com/video");
        } catch (IllegalArgumentException expected) {
            // Expected: domain rejected before yt-dlp is called
        }

        // yt-dlp should never be invoked for invalid domains
        verify(mockYoutube, never()).GetVideoInfo("https://evil.com/video");
    }

    @Test
    void errorMessageContainsDomainName() {
        Youtube mockYoutube = mock(Youtube.class);
        YoutubeService service = new YoutubeService(mockYoutube, testDomains);

        assertThatThrownBy(() -> service.getVideoInfo("https://vimeo.com/123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vimeo.com")
            .hasMessageContaining("not an allowed YouTube domain");
    }
}
