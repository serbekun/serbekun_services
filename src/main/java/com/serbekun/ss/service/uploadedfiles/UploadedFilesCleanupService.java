package com.serbekun.ss.service.uploadedfiles;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background service that periodically scans uploaded files
 * and removes those whose {@code expiredTime} has passed.
 * <p>
 * Uses a single-threaded scheduled executor, similar to {@link com.serbekun.ss.service.autosave.AutosaveService}.
 */
public class UploadedFilesCleanupService {

    // region constants

    /** Logger for the cleanup service. */
    private static final Logger log = LoggerFactory.getLogger(UploadedFilesCleanupService.class);

    /** The uploaded files service used to delete expired files. */
    private final UploadedFilesService service;
    /** The interval in seconds at which to run the cleanup check. */
    private final long intervalSeconds;
    /** The scheduled executor service for running the cleanup task. */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // endregion


    // region methods

    /**
     * @param service         the uploaded files service used to delete expired files.
     * @param intervalSeconds how often to run the cleanup check.
     */
    public UploadedFilesCleanupService(UploadedFilesService service, long intervalSeconds) {
        this.service = service;
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * Start the periodic cleanup task.
     */
    public void start() {
        log.info("Starting uploaded-files cleanup service (interval={}s)", intervalSeconds);
        scheduler.scheduleAtFixedRate(
                this::cleanup,
                intervalSeconds,   // initial delay
                intervalSeconds,   // period
                TimeUnit.SECONDS
        );
    }

    /**
     *  Stop the cleanup scheduler. 
     */
    public void stop() {
        log.info("Stopping uploaded-files cleanup service");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform the cleanup operation.
     */
    private void cleanup() {
        try {
            service.deleteExpiredFiles();
        } catch (Exception e) {
            log.error("Error during uploaded-files cleanup cycle", e);
        }
    }

    // endregion
}
