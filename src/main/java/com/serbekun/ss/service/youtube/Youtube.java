package com.serbekun.ss.service.youtube;

import java.io.IOException;
import com.serbekun.ss.config.Paths;

public class Youtube {
    
    /**
     * 
     * Download video from Youtube by url
     * 
     * @param url youtube video url
     * @return video bytes, or null if download failed
     * @throws IOException if process cannot be started or interrupted
     * @throws IllegalStateException if yt-dlp command fails
     */
    public static byte[] DownloadVideoByUrl(String url) throws IOException {

        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "-f", "best",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            "-o", "-",
            url
        );
        
        Process process = pb.start();
        
        try {
            byte[] videoBytes = process.getInputStream().readAllBytes();
            
            // Wait for process to complete and check exit code
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                throw new IllegalStateException(
                    "yt-dlp failed with exit code " + exitCode + ": " + errorOutput
                );
            }
            
            return videoBytes;
            
        } catch (InterruptedException e) {
            process.destroy();
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }
    
    /**
     * Get video metadata from Youtube by url.
     *
     * @param url youtube video url
     * @return JSON string with video metadata
     * @throws IOException if process cannot be started or interrupted
     * @throws IllegalStateException if yt-dlp command fails
     */
    public static String GetVideoInfo(String url) throws IOException {

        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "--dump-json",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            url
        );
        
        Process process = pb.start();
        
        try {
            String jsonOutput = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                throw new IllegalStateException(
                    "yt-dlp failed with exit code " + exitCode + ": " + errorOutput
                );
            }
            
            return jsonOutput;
            
        } catch (InterruptedException e) {
            process.destroy();
            Thread.currentThread().interrupt();
            throw new IOException("Video info fetch interrupted", e);
        }
    }
}
