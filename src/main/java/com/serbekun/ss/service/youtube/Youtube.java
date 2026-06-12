package com.serbekun.ss.service.youtube;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.serbekun.ss.config.Paths;

public class Youtube {
    
    private static final long PROCESS_TIMEOUT_SECONDS = 120;
    private static final String DENO_PATH = "/home/sergei/.deno/bin";
    private static final String LOCAL_BIN_PATH = "/home/sergei/.local/bin";
    
    private static void setupEnvironment(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        String existingPath = env.getOrDefault("PATH", "");
        env.put("PATH", DENO_PATH + ":" + LOCAL_BIN_PATH + ":" + existingPath);
    }
    
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
            "--js-runtimes", "deno",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            "-o", "-",
            url
        );
        
        // Add required paths for yt-dlp process
        setupEnvironment(pb);
        
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
            "--js-runtimes", "deno",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            url
        );
        
        // Add required paths for yt-dlp process
        setupEnvironment(pb);
        
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
