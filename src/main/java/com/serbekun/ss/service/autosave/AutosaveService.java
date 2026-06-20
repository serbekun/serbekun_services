package com.serbekun.ss.service.autosave;

// java imports
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.serbekun.ss.service.autosave.interfaces.AutoSavable;


/**
 * Background service responsible for periodically calling {@link AutoSavable#save()}
 * on registered components (usually repositories) to persist in-memory data to disk.
 * <p>
 * Uses a single-threaded scheduled executor to avoid thread contention.
 * Thread-safe registration thanks to {@link CopyOnWriteArrayList}.
 * </p>
 * <p>
 * Typical usage:
 * <pre>
 * AutosaveService autosave = new AutosaveService();
 * autosave.register(playerRepository);
 * autosave.register(shopRepository);
 * autosave.start();
 *
 * // ... application runs ...
 *
 * // on shutdown
 * autosave.stop();
 * </pre>
 * </p>
 */
public class AutosaveService {

    /**
     * List of repository {@link com.serbekun.ss.repo} that implement {@link com.serbekun.ss.service.autosave.interfaces.AutoSavable}
     * and will be automatically saved by this thread
     */
    private final List<AutoSavable> savable = new CopyOnWriteArrayList<>();

    /**
     * An ExecutorService that can schedule commands to run after a given delay, or to execute periodically.
     */
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    /**
     * 
     * register repository for automatically call {@link com.serbekun.ss.service.autosave.interfaces.AutoSavable#save()}
     * 
     * @param repository repository object that implement {@link com.serbekun.ss.service.autosave.interfaces.AutoSavable}
     */
    public void register(AutoSavable repository) {
        savable.add(repository);
    }

    /**
     * Start auto save thread that will be call all 
     * {@link com.serbekun.ss.service.autosave.interfaces.AutoSavable#save()}
     * in the {@link AutosaveService#savable} object 
     * for save data from server memory to files
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            for (AutoSavable s : savable) {
                try {
                    s.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 20, 20, TimeUnit.SECONDS);
    }

    /**
     * Stop thread that start {@link AutosaveService#start()}
     */
    public void stop() {
        scheduler.shutdown();
    }
}