package com.serbekun.ss.service.Youtube;

import java.io.IOException;

public class Youtube {
    
    /**
     * 
     * Download video from Youtube by url
     * 
     * @param url youtube video url
     * @return video bytes
     */
    public static byte[] DownloadVideoByUrl(String url) {

        try {

            Process process = new ProcessBuilder(
                "yt-dlp",
                "-f", "bestvideo+bestaudio",
                "--merge-output-format", "mp4",
                "-o", "-",
                url
            ).start();
            
            return process.getInputStream().readAllBytes();

        } catch (IOException e) {
            return null;
        }
    }
}
