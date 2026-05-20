package com.serbekun;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.core.Config;
import com.serbekun.ss.core.Paths;
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

        var linksRepo = new LinksRepository(Paths.LinksConfig.getLinksStorageFile());
        var tokensRepo = new EndpointAccessTokensRepository(
            Paths.TokensConfig.getTokensStorageFolder());
        var linksLocalTokensRepo = new LocalTokensRepository(
            Paths.LinksConfig.getLinksLocalTokensStorageFile(),
            "ss_links-");

        return new Repositories(
            linksRepo,
            tokensRepo,
            linksLocalTokensRepo,
            linksRepo.getLinks(),
            tokensRepo.getEndpointAccessTokens(),
            linksLocalTokensRepo.getTokens()
        );
    }

    private static Services initializeServices(Repositories repos) {
        log.info("Initializing services");

        var endpointRegistry = new EndpointRegistry();
        var tokensService = new EndpointAccessTokensService(repos.endpointTokens);
        var linksService = new LinksService(repos.links);
        var authService = new AuthService(tokensService, endpointRegistry);

        return new Services(endpointRegistry, tokensService, linksService, authService);
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
            new StaticV0Json(resources.resourcesService),
            new StaticV0Images(resources.resourcesService),
            new StaticV0Html(resources.resourcesService),
            new ApiV0CatalogsLinks(services.linksService, repos.linksLocalTokens),
            new ApiV0CipherAes()
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
            ctx.handlers.json,
            ctx.handlers.images,
            ctx.handlers.html,
            ctx.handlers.links,
            ctx.handlers.cipher
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
        autosave.register(repos.linksRepo);
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
        private final com.serbekun.ss.core.Links links;
        private final com.serbekun.ss.core.EndpointsAccessTokens endpointTokens;
        private final com.serbekun.ss.core.LocalTokens linksLocalTokens;

        private Repositories(
                LinksRepository linksRepo,
                EndpointAccessTokensRepository endpointTokensRepo,
                LocalTokensRepository linksLocalTokensRepo,
                com.serbekun.ss.core.Links links,
                com.serbekun.ss.core.EndpointsAccessTokens endpointTokens,
                com.serbekun.ss.core.LocalTokens linksLocalTokens) {
            this.linksRepo = linksRepo;
            this.endpointTokensRepo = endpointTokensRepo;
            this.linksLocalTokensRepo = linksLocalTokensRepo;
            this.links = links;
            this.endpointTokens = endpointTokens;
            this.linksLocalTokens = linksLocalTokens;
        }
    }

    private static final class Services {
        private final EndpointRegistry endpointRegistry;
        private final EndpointAccessTokensService tokensService;
        private final LinksService linksService;
        private final AuthService authService;

        private Services(
                EndpointRegistry endpointRegistry,
                EndpointAccessTokensService tokensService,
                LinksService linksService,
                AuthService authService) {
            this.endpointRegistry = endpointRegistry;
            this.tokensService = tokensService;
            this.linksService = linksService;
            this.authService = authService;
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
        private final StaticV0Json json;
        private final StaticV0Images images;
        private final StaticV0Html html;
        private final ApiV0CatalogsLinks links;
        private final ApiV0CipherAes cipher;

        private Handlers(
                StaticV0Json json,
                StaticV0Images images,
                StaticV0Html html,
                ApiV0CatalogsLinks links,
                ApiV0CipherAes cipher) {
            this.json = json;
            this.images = images;
            this.html = html;
            this.links = links;
            this.cipher = cipher;
        }
    }
}
