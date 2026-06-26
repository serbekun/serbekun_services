package com.serbekun.ss.service.youtube;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.Map;

import com.serbekun.ss.config.Paths;

public class Youtube {

    private final long processTimeoutSeconds;
    private final String ytDlpPath;
    private final String denoPath;

    public Youtube(long processTimeoutSeconds, String ytDlpPath, String denoPath) {
        this.processTimeoutSeconds = processTimeoutSeconds;
        this.ytDlpPath = ytDlpPath;
        this.denoPath = denoPath;
    }

    /**
     * Validates that the given string looks like a real URL,
     * not a yt-dlp option or anything else.
     *
     * @param url the url to validate
     * @throws IllegalArgumentException if url is not a valid http/https URL
     */
    static void validateUrl(String url) {

        // Basic validation to ensure the URL is not empty,
        // does not start with a dash (which could be interpreted as a command-line option),
        // and starts with "http://" or "https://".
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be empty");
        }
        if (url.startsWith("-")) {
            throw new IllegalArgumentException("URL must not start with '-', looks like a CLI option");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("URL must start with http:// or https://");
        }
    }

    private void setupEnvironment(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        String existingPath = env.getOrDefault("PATH", "");
        env.put("PATH", denoPath + ":" + existingPath);
    }

    public byte[] DownloadVideoByUrl(String url) throws IOException {
        // Validate the URL before proceeding
        validateUrl(url);

        // Prepare the yt-dlp command with necessary arguments
        ProcessBuilder pb = new ProcessBuilder(
            ytDlpPath,
            "-f", "best",
            "--no-playlist",
            "--js-runtimes", "deno",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            "-o", "-",
            "--",
            url
        );

        // Add required paths for yt-dlp process
        setupEnvironment(pb);

        // Start the process and handle its output
        Process process = pb.start();
        
        // Read the output bytes from the process's input stream and handle timeouts and errors
        try {
            byte[] videoBytes = process.getInputStream().readAllBytes();
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("yt-dlp download timed out after " + processTimeoutSeconds + " seconds");
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
    public String GetVideoInfo(String url) throws IOException {
        validateUrl(url);

        // Prepare the yt-dlp command to fetch video info in JSON format
        ProcessBuilder pb = new ProcessBuilder(
            ytDlpPath,
            "--dump-json",
            "--no-playlist",
            "--js-runtimes", "deno",
            "--cookies", Paths.YoutubeConfig.getCookiesPath().toString(),
            "--",
            url
        );
        
        // Add required paths for yt-dlp process
        setupEnvironment(pb);
        
        Process process = pb.start();
        
        // Read the output from the process's input stream and handle timeouts and errors
        try {
            String jsonOutput = new String(process.getInputStream().readAllBytes());
            
            // Wait for process to complete with timeout
            boolean completed = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("yt-dlp info fetch timed out after " + processTimeoutSeconds + " seconds");
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
