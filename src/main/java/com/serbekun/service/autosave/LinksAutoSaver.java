package com.serbekun.service.autosave;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.serbekun.core.Links;
import com.serbekun.repository.LinksRepository;

/**
 * class for run links autosave thread
 */
public class LinksAutoSaver {

    private final Links links;
    private final LinksRepository repository;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructor for LinksAutoSaver.
     * @param links the Links instance to save
     * @param repository the repository to use for saving
     */
    public LinksAutoSaver(Links links, LinksRepository repository) {
        this.links = links;
        this.repository = repository;
    }

    /**
     * Starts the autosave thread that periodically saves links.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            repository.save(links.getAllLinks());
        }, 20, 20, TimeUnit.SECONDS);
    }

    /**
     * Stops the autosave thread.
     */
    public void stop() {
        scheduler.shutdown();
    }
}