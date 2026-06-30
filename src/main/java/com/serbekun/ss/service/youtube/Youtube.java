package com.serbekun.ss.service.youtube;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * Runs the given yt-dlp process, returning the raw bytes written to stdout.
     *
     * <p>stderr is drained on a separate thread concurrently with reading stdout.
     * If we read stdout to completion before touching stderr (as a naive
     * implementation does), a large amount of output on stderr can fill the OS
     * pipe buffer and block the child process, which in turn blocks our stdout
     * read — a deadlock that only resolves when the timeout forcibly kills the
     * process. Draining both streams in parallel avoids that.
     *
     * @param pb            configured process builder (environment is set up here)
     * @param operationName short label used in timeout/interrupt error messages
     * @return bytes captured from the process stdout
     * @throws IOException           if the process cannot be started, times out, or is interrupted
     * @throws IllegalStateException if yt-dlp exits with a non-zero code
     */
    private byte[] runProcess(ProcessBuilder pb, String operationName) throws IOException {
        setupEnvironment(pb);

        Process process = pb.start();

        // Drain stderr concurrently so the child never blocks on a full stderr buffer.
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        Thread errReader = new Thread(() -> {
            try (InputStream err = process.getErrorStream()) {
                err.transferTo(errBuffer);
            } catch (IOException ignored) {
                // Stream closed because the process ended; nothing actionable here.
            }
        }, "yt-dlp-stderr-reader");
        errReader.setDaemon(true);
        errReader.start();

        try {
            byte[] output = process.getInputStream().readAllBytes();

            boolean completed = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("yt-dlp " + operationName + " timed out after " + processTimeoutSeconds + " seconds");
            }

            // Process has exited, so stderr is at EOF; make sure it is fully drained
            // before we read the captured bytes.
            errReader.join(TimeUnit.SECONDS.toMillis(processTimeoutSeconds));

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorOutput = errBuffer.toString(StandardCharsets.UTF_8);
                throw new IllegalStateException(
                    "yt-dlp failed with exit code " + exitCode + ": " + errorOutput
                );
            }

            return output;

        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("yt-dlp " + operationName + " interrupted", e);
        }
    }

    public byte[] downloadVideoByUrl(String url) throws IOException {
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

        return runProcess(pb, "download");
    }
    
    /**
     * Get video metadata from Youtube by url.
     *
     * @param url youtube video url
     * @return JSON string with video metadata
     * @throws IOException if process cannot be started or interrupted
     * @throws IllegalStateException if yt-dlp command fails
     */
    public String getVideoInfo(String url) throws IOException {
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
        
        return new String(runProcess(pb, "info fetch"), StandardCharsets.UTF_8);
    }
}
