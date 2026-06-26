package com.serbekun.ss.service.youtube;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YoutubeService {

    // region Fields

    /** The JSON mapper for parsing YouTube responses. */
    public final ObjectMapper mapper = new ObjectMapper();

    /** The logger for this class.*/
    private static final Logger log = LoggerFactory.getLogger(YoutubeService.class);

    /** The YouTube client. */
    private final Youtube youtube;
    /** The allowed YouTube domains. */
    private final YoutubeDomains youtubeDomains;

    // endregion

    public YoutubeService(Youtube youtube, YoutubeDomains youtubeDomains) {
        this.youtube = youtube;
        this.youtubeDomains = youtubeDomains;
    }

    /**
     * Validates that the URL's host belongs to an allowed YouTube domain.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if domain is not allowed
     */
    private void validateDomain(String url) {
        if (!youtubeDomains.isAllowed(url)) {
            String host = YoutubeDomains.extractHost(url);
            throw new IllegalArgumentException(
                "Domain \"" + (host != null ? host : url) + "\" is not an allowed YouTube domain"
            );
        }
    }

    /**
     * Downloads a video from YouTube using the provided URL.
     * @param url the YouTube URL
     * @return the downloaded video as a byte array
     * @throws IOException if the download fails
     */
    public byte[] downloadVideo(String url) throws IOException {
        validateDomain(url);
        try {
            return youtube.DownloadVideoByUrl(url);
        } catch (IllegalStateException e) {
            log.error("yt-dlp download failed: {}", e.getMessage());
            throw new IOException("Failed to download video");
        }
    }

    /**
     * Fetches video information from YouTube using the provided URL.
     * @param url the YouTube URL
     * @return the video information as a JSON string
     */
    public String getVideoInfo(String url) {
        validateDomain(url);
        String string;
        try {
            string = youtube.GetVideoInfo(url);
        } catch (Exception e) {
            log.error("yt-dlp info failed: {}", e.getMessage());
            throw new RuntimeException("Maybe you download video that required login to youtube.");
        }

        try {
            mapper.readValue(string, YtDlpError.class);
        } catch (Exception e) {
            return string;
        }

        log.error("yt-dlp returned error JSON: {}", string);
        throw new RuntimeException("Maybe you download video that required login to youtube.");
    }
}
