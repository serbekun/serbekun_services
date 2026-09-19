package com.serbekun.ss.service.burnlink;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background service that periodically removes burn links whose deadline has
 * passed, mirroring {@link com.serbekun.ss.service.uploadedfiles.UploadedFilesCleanupService}.
 */
public class BurnLinkCleanupService {

    private static final Logger log = LoggerFactory.getLogger(BurnLinkCleanupService.class);

    private final BurnLinkService service;
    private final long intervalSeconds;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BurnLinkCleanupService(BurnLinkService service, long intervalSeconds) {
        this.service = service;
        this.intervalSeconds = intervalSeconds;
    }

    /** Start the periodic cleanup task. */
    public void start() {
        log.info("Starting burn-link cleanup service (interval={}s)", intervalSeconds);
        scheduler.scheduleAtFixedRate(
                this::cleanup,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS);
    }

    /** Stop the cleanup scheduler. */
    public void stop() {
        log.info("Stopping burn-link cleanup service");
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

    private void cleanup() {
        try {
            service.deleteExpired();
        } catch (Exception e) {
            log.error("Error during burn-link cleanup cycle", e);
        }
    }
}
