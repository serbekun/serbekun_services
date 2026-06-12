package com.serbekun.ss.service.youtube;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.serbekun.ss.config.Paths;

public class Youtube {
    
    private static final long PROCESS_TIMEOUT_SECONDS = 120;
    
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
            "--no-playlist",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            "-o", "-",
            url
        );
        
        Process process = pb.start();
        
        try {
            byte[] videoBytes = process.getInputStream().readAllBytes();
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("yt-dlp download timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                throw new IllegalStateException(
                    "yt-dlp failed with exit code " + exitCode + ": " + errorOutput
                );
            }
            
            return videoBytes;
            
        } catch (InterruptedException e) {
            process.destroyForcibly();
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
            "--no-playlist",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            url
        );
        
        Process process = pb.start();
        
        try {
            String jsonOutput = new String(process.getInputStream().readAllBytes());
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("yt-dlp info fetch timed out after " + PROCESS_TIMEOUT_SECONDS + " seconds");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorOutput = new String(process.getErrorStream().readAllBytes());
                throw new IllegalStateException(
                    "yt-dlp failed with exit code " + exitCode + ": " + errorOutput
                );
            }
            
            return jsonOutput;
            
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Video info fetch interrupted", e);
        }
    }
}
