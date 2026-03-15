package com.serbekun.service.autosave;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.serbekun.service.tokens.TokensService;
import com.serbekun.repository.TokensRepository;

/**
 * class for run tokens autosave thread
 */
public class TokensAutoSave {

    private final TokensService tokensService;
    private final TokensRepository repository;

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructor for TokensAutoSave.
     * @param tokensService the TokensService instance to save
     * @param repository the repository to use for saving
     */
    public TokensAutoSave(TokensService tokensService, TokensRepository repository) {
        this.tokensService = tokensService;
        this.repository = repository;
    }

    /**
     * Starts the autosave thread that periodically saves tokens.
     */
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            repository.save(tokensService.getAllTokens());
        }, 20, 20, TimeUnit.SECONDS);
    }

    /**
     * Stops the autosave thread.
     */
    public void stop() {
        scheduler.shutdown();
    }
}