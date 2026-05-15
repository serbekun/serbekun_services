package com.serbekun;


import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.config.core.CoreConfig;
import com.serbekun.ss.core.*;
import com.serbekun.ss.http.handles.InitHandles;
import com.serbekun.ss.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.ss.repository.*;
import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.EndpointRegistry;
import com.serbekun.ss.service.autosave.*;
import com.serbekun.ss.service.http.handles.v0.*;
import com.serbekun.ss.service.links.LinksService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.tokens.EndpointAccessTokensService;

import io.javalin.Javalin;

public class Main {
    
    /**
     * Main entry point of the application.
     * Initializes server storage, repositories, services, autosave, HTTP handlers,
     * starts Javalin server on port 8080 and autosave service.
     * Adds shutdown hook for graceful server stop.
     */
    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(Main.class);
        
        // 1. Initialize server storage folders
        log.info("Initializing server storage folders");
        ServerStorageInitializer storageInitializer = new ServerStorageInitializer();
        storageInitializer.initialize(Path.of(CoreConfig.Infrastructure.Fs.getServerStorageFolder()));
        
        // 2. Initialize repositories
        log.info("Initializing repositories");
        LinksRepository linksRepository = new LinksRepository(CoreConfig.LinksConfig.getLinksStorageFile());
        EndpointAccessTokensRepository tokensRepository = new EndpointAccessTokensRepository(
            CoreConfig.TokensConfig.getTokensStorageFolder()
        );
        LocalTokensRepository localTokensRepository = new LocalTokensRepository(
            CoreConfig.LinksConfig.getLinksLocalTokensStorageFile()
        );
        
        // Load core data from repositories
        Links links = linksRepository.getLinks();
        EndpointsAccessTokens endpointTokens = tokensRepository.getEndpointAccessTokens();
        LocalTokens localTokens = localTokensRepository.getTokens();
        
        // 3. Initialize services
        log.info("Initializing services");
        EndpointRegistry endpointRegistry = new EndpointRegistry();
        EndpointAccessTokensService tokensService = new EndpointAccessTokensService(endpointTokens);
        LinksService linksService = new LinksService(links);
        AuthService authService = new AuthService(tokensService, endpointRegistry);
        
        // 4. Initialize autosave service and register repositories
        log.info("Initializing autosave service");
        AutosaveService autosaveService = new AutosaveService();
        autosaveService.register(tokensRepository);
        autosaveService.register(linksRepository);
        
        // 5. Initialize resources
        log.info("Initializing resources");
        ResourceLoader resourceLoader = new ResourceLoader();
        ResourceCache resourceCache = new ResourceCache(resourceLoader);
        ResourcesService resourcesService = new ResourcesService(resourceLoader, resourceCache);
        
        // 6. Initialize Javalin server
        log.info("Initializing Javalin server");
        Javalin server = Javalin.create();
        
        // 7. Initialize HTTP handlers
        StaticV0Json jsonHandler = new StaticV0Json(resourcesService);
        StaticV0Images imagesHandler = new StaticV0Images(resourcesService);
        StaticV0Html htmlHandler = new StaticV0Html(resourcesService);
        ApiV0CatalogsLinks linksHandler = new ApiV0CatalogsLinks(linksService, localTokens);
        ApiV0CipherAes cipherHandler = new ApiV0CipherAes();
        
        InitHandles initHandles = new InitHandles();
        initHandles.initHandles(
            server, resourcesService, authService, endpointRegistry,
            jsonHandler, imagesHandler, htmlHandler, linksHandler, cipherHandler
        );
        
        // 8. Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            autosaveService.stop(); // Ensure autosave stops
            server.stop();
        }));
        
        // 9. Start HTTP server and autosave in separate threads
        log.info("Starting server on port 8080");
        Thread httpThread = new Thread(() -> server.start(8080), "http-server");
        httpThread.start();
        
        autosaveService.start();
        log.info("Server started successfully");
    }
}