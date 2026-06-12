package com.serbekun;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.config.Config;
import com.serbekun.ss.config.Paths;
import com.serbekun.ss.http.handles.InitHandles;
import com.serbekun.ss.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.ss.repository.*;
import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.EndpointRegistry;
import com.serbekun.ss.service.autosave.*;

import com.serbekun.ss.service.links.LinksService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.tokens.EndpointAccessTokensService;
import com.serbekun.ss.service.youtube.YoutubeService;
import com.serbekun.ss.domain.models.EndpointsAccessTokens;
import com.serbekun.ss.domain.models.LinksRepository;
import com.serbekun.ss.domain.models.LinksRepositoryReadInterface;
import com.serbekun.ss.domain.models.LocalTokens;

import io.javalin.Javalin;

public class Main {
    
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    
    /**
     * Main entry point of the application.
     * Initializes server storage, repositories, services, autosave, HTTP handlers,
     * starts Javalin server on port 8080 and autosave service.
     * Adds shutdown hook for graceful server stop.
     */
    public static void main(String[] args) {
        log.info("Starting Serbekun server...");

        // 0. Config
        Config config = loadConfig();

        ServerContext context = initializeApplication();

        startServer(context, config);

        log.info("Server started successfully on port {}", config.getPort());
    }

    private static Config loadConfig() {
        log.info("Loading server config");
        return Config.load(Path.of(Paths.Infrastructure.Fs.getServerStorageFolder(), "config.json"));
    }

    private static ServerContext initializeApplication() {
        // 1. Storage
        initializeStorage();

        // 2. Repositories + data
        Repositories repos = initializeRepositories();

        // 3. Services
        Services services = initializeServices(repos);

        // 4. Resources
        Resources resources = initializeResources();

        // 5. HTTP handlers
        Handlers handlers = initializeHandlers(services, resources, repos);

        return new ServerContext(repos, services, resources, handlers);
    }

    private static void initializeStorage() {
        log.info("Initializing server storage folders");
        new ServerStorageInitializer()
            .initialize(Path.of(Paths.Infrastructure.Fs.getServerStorageFolder()));
    }

    private static Repositories initializeRepositories() {
        log.info("Initializing repositories");

        // Initialize FileRepos for read data from files
        LinksFileRepository linksFileRepo = new LinksFileRepository(Paths.LinksConfig.getLinksStorageFile());

        EndpointAccessTokensRepositoryImpl endpointAccessTokensRepo = new EndpointAccessTokensRepositoryImpl(Paths.TokensConfig.getTokensStorageFolder());
        LocalTokensRepositoryImpl linksLocalTokensRepo = new LocalTokensRepositoryImpl(Paths.LinksConfig.getLinksLocalTokensStorageFile(),"ss_links-");

        // Load data to repository
        LinksRepository linksRepository = new LinksRepository(linksFileRepo.load());

        // Give Interface to read repos data
        linksFileRepo.setLinksRepositoryReadInterface(linksRepository);

        return new Repositories(linksRepository, endpointAccessTokensRepo, linksLocalTokensRepo, linksFileRepo);
    }

    private static Services initializeServices(Repositories repos) {
        var endpointRegistry = new EndpointRegistry();
        var endpointAccessTokensService = new EndpointAccessTokensService(repos.endpointTokensRepo.getEndpointAccessTokens());
            var linksService = new LinksService(repos.linksRepo, repos.linksLocalTokensRepo.getTokens());
        var authService = new AuthService(endpointAccessTokensService, endpointRegistry);
        var youtubeService = new YoutubeService();
        return new Services(endpointRegistry, endpointAccessTokensService, linksService, authService, youtubeService);
    }

    private static Resources initializeResources() {
        log.info("Initializing resources");
        var resourceLoader = new ResourceLoader();
        var resourceCache = new ResourceCache(resourceLoader);
        var resourcesService = new ResourcesService(resourceLoader, resourceCache);

        return new Resources(resourceLoader, resourceCache, resourcesService);
    }

    private static Handlers initializeHandlers(Services services, Resources resources, Repositories repos) {
        log.info("Initializing HTTP handlers");

        return new Handlers(
            resources.resourcesService,
            services.linksService,
            new com.serbekun.ss.service.cipher.CipherService(),
            services.youtubeService
        );
    }

    private static void startServer(ServerContext ctx, Config config) {
        log.info("Initializing Javalin server");
        Javalin server = Javalin.create();

        InitHandles initHandles = new InitHandles();
        initHandles.initHandles(
            server,
            ctx.resources.resourcesService,
            ctx.services.authService,
            ctx.services.endpointRegistry,
            ctx.handlers.resourcesService,
            ctx.handlers.linksService,
            ctx.handlers.cipherService,
            ctx.handlers.youtubeService
        );

        // Autosave
        AutosaveService autosaveService = createAndStartAutosave(ctx.repos);

        addShutdownHook(server, autosaveService);

        // Run
        server.start(config.getPort());
    }

    private static AutosaveService createAndStartAutosave(Repositories repos) {
        log.info("Initializing autosave service");
        var autosave = new AutosaveService();
        autosave.register(repos.linksFileRepo);
        autosave.register(repos.endpointTokensRepo);
        autosave.register(repos.linksLocalTokensRepo);
        autosave.start();
        return autosave;
    }

    private static void addShutdownHook(Javalin server, AutosaveService autosave) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            autosave.stop();
            server.stop();
        }, "shutdown-hook"));
    }

    private static final class ServerContext {
        private final Repositories repos;
        private final Services services;
        private final Resources resources;
        private final Handlers handlers;

        private ServerContext(Repositories repos, Services services, Resources resources, Handlers handlers) {
            this.repos = repos;
            this.services = services;
            this.resources = resources;
            this.handlers = handlers;
        }
    }

    private static final class Repositories {
        private final LinksRepository linksRepo;
        private final EndpointAccessTokensRepository endpointTokensRepo;
        private final LocalTokensRepository linksLocalTokensRepo;
        private final LinksFileRepository linksFileRepo;

        private Repositories(
                LinksRepository linksRepo,
                EndpointAccessTokensRepository endpointTokensRepo,
                LocalTokensRepository linksLocalTokensRepo,
                LinksFileRepository linksFileRepo) {
            this.linksRepo = linksRepo;
            this.endpointTokensRepo = endpointTokensRepo;
            this.linksLocalTokensRepo = linksLocalTokensRepo;
            this.linksFileRepo = linksFileRepo;
        }
    }

    private static final class Services {
        private final EndpointRegistry endpointRegistry;
        private final EndpointAccessTokensService tokensService;
        private final LinksService linksService;
        private final AuthService authService;
        private final YoutubeService youtubeService;

        private Services(
                EndpointRegistry endpointRegistry,
                EndpointAccessTokensService tokensService,
                LinksService linksService,
                AuthService authService,
                YoutubeService youtubeService) {
            this.endpointRegistry = endpointRegistry;
            this.tokensService = tokensService;
            this.linksService = linksService;
            this.authService = authService;
            this.youtubeService = youtubeService;
        }
    }

    private static final class Resources {
        private final ResourceLoader resourceLoader;
        private final ResourceCache resourceCache;
        private final ResourcesService resourcesService;

        private Resources(
                ResourceLoader resourceLoader,
                ResourceCache resourceCache,
                ResourcesService resourcesService) {
            this.resourceLoader = resourceLoader;
            this.resourceCache = resourceCache;
            this.resourcesService = resourcesService;
        }
    }

    private static final class Handlers {
        private final com.serbekun.ss.service.resource.ResourcesService resourcesService;
        private final com.serbekun.ss.service.links.LinksService linksService;
        private final com.serbekun.ss.service.cipher.CipherService cipherService;
        private final YoutubeService youtubeService;

        private Handlers(
                com.serbekun.ss.service.resource.ResourcesService resourcesService,
                com.serbekun.ss.service.links.LinksService linksService,
                com.serbekun.ss.service.cipher.CipherService cipherService,
                YoutubeService youtubeService) {
            this.resourcesService = resourcesService;
            this.linksService = linksService;
            this.cipherService = cipherService;
            this.youtubeService = youtubeService;
        }
    }
}
