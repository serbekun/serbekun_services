package com.serbekun.ss.service.youtube;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public class YoutubeService {

    public final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(YoutubeService.class);
    
    public byte[] downloadVideo(String url) throws IOException {
        try {
            return Youtube.DownloadVideoByUrl(url);
        } catch (IllegalStateException e) {
            log.error("yt-dlp download failed: {}", e.getMessage());
            throw new IOException("Failed to download video");
        }
    }

    public String getVideoInfo(String url) {
        String string;
        try {
            string = Youtube.GetVideoInfo(url);
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
