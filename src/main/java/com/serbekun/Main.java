package com.serbekun;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javalin.Javalin;

import com.serbekun.ss.config.Config;
import com.serbekun.ss.config.Paths;
import com.serbekun.ss.http.handles.InitHandles;
import com.serbekun.ss.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensFileRepo;
import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.repo.links.LinksFileRepo;
import com.serbekun.ss.repo.links.LinksRepo;
import com.serbekun.ss.repo.localtokens.LocalTokensFileRepo;
import com.serbekun.ss.repo.localtokens.LocalTokensRepo;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesFileRepo;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesRepo;
import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.EndpointRegistry;
import com.serbekun.ss.service.autosave.*;
import com.serbekun.ss.service.links.LinksService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.tokens.EndpointsAccessTokensService;
import com.serbekun.ss.service.youtube.Youtube;
import com.serbekun.ss.service.youtube.YoutubeService;


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

        ServerContext context = initializeApplication(config);

        startServer(context, config);

        log.info("Server started successfully on port {}", config.getPort());
    }

    private static Config loadConfig() {
        log.info("Loading server config");
        return Config.load(Path.of(Paths.Infrastructure.Fs.getServerStorageFolder(), "config.json"));
    }

    private static ServerContext initializeApplication(Config config) {
        // 1. Storage
        initializeStorage();

        // 2. Repositories + data
        Repositories repos = initializeRepositories();

        // 3. Services
        Services services = initializeServices(repos, config);

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

        // 1. Links
        LinksFileRepo linksFileRepo = new LinksFileRepo(Paths.LinksConfig.getLinksStorageFile());
        LinksRepo linksRepo = new LinksRepo(linksFileRepo.load());
        linksFileRepo.setLinksRepositoryReadInterface(linksRepo);

        // 2. Endpoint Access Tokens
        EndpointsAccessTokensFileRepo endpointsAccessTokensFileRepo = new EndpointsAccessTokensFileRepo(Paths.TokensConfig.getTokensStorageFolder());
        EndpointsAccessTokensRepo endpointsAccessTokensRepo = new EndpointsAccessTokensRepo(endpointsAccessTokensFileRepo.load());
        endpointsAccessTokensFileRepo.setEndpointsAccessTokensFileRepository(endpointsAccessTokensRepo);

        // 3. Local Tokens (for links)
        LocalTokensFileRepo linksLocalTokensFileRepo = new LocalTokensFileRepo(Paths.LinksConfig.getLinksLocalTokensStorageFile(), "ss_links-");
        LocalTokensRepo linkLocalTokens = new LocalTokensRepo(linksLocalTokensFileRepo.load(), "ss_links-");
        linksLocalTokensFileRepo.setLocalTokensReadInterface(linkLocalTokens);

        // 4. Uploaded Files
        UploadedFilesFileRepo uploadedFilesFileRepo = new UploadedFilesFileRepo(Paths.UploadedFilesConfig.getUploadedFilesStorageFile());
        UploadedFilesRepo uploadedFilesRepo = new UploadedFilesRepo(uploadedFilesFileRepo.load());
        uploadedFilesFileRepo.setUploadedFilesReadInterface(uploadedFilesRepo);

        return new Repositories(
            linksRepo,
            endpointsAccessTokensRepo,
            endpointsAccessTokensFileRepo,
            linkLocalTokens,
            linksLocalTokensFileRepo,
            linksFileRepo,
            uploadedFilesRepo,
            uploadedFilesFileRepo
        );
    }

    private static Services initializeServices(Repositories repos, Config config) {
        var endpointRegistry = new EndpointRegistry();
        var endpointAccessTokensService = new EndpointsAccessTokensService(repos.endpointsAccessTokensRepo);
        var linksService = new LinksService(repos.linksRepo, repos.linkLocalTokens);
        var authService = new AuthService(endpointAccessTokensService, endpointRegistry);
        var youtube = new Youtube(
            config.getYoutubeProcessTimeoutSeconds(),
            config.getYtDlpPath(),
            config.getDenoPath()
        );
        var youtubeService = new YoutubeService(youtube);
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
        autosave.register(repos.endpointsAccessTokensFileRepo);
        autosave.register(repos.linksLocalTokensFileRepo);
        autosave.register(repos.uploadedFilesFileRepo);
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
        private final LinksRepo linksRepo;
        private final EndpointsAccessTokensRepo endpointsAccessTokensRepo;
        private final EndpointsAccessTokensFileRepo endpointsAccessTokensFileRepo;
        private final LocalTokensRepo linkLocalTokens;
        private final LocalTokensFileRepo linksLocalTokensFileRepo;
        private final LinksFileRepo linksFileRepo;
        private final UploadedFilesRepo uploadedFilesRepo;
        private final UploadedFilesFileRepo uploadedFilesFileRepo;

        private Repositories(
                LinksRepo linksRepo,
                EndpointsAccessTokensRepo endpointsAccessTokensRepo,
                EndpointsAccessTokensFileRepo endpointsAccessTokensFileRepo,
                LocalTokensRepo linkLocalTokens,
                LocalTokensFileRepo linksLocalTokensFileRepo,
                LinksFileRepo linksFileRepo,
                UploadedFilesRepo uploadedFilesRepo,
                UploadedFilesFileRepo uploadedFilesFileRepo) {
            this.linksRepo = linksRepo;
            this.endpointsAccessTokensRepo = endpointsAccessTokensRepo;
            this.endpointsAccessTokensFileRepo = endpointsAccessTokensFileRepo;
            this.linkLocalTokens = linkLocalTokens;
            this.linksLocalTokensFileRepo = linksLocalTokensFileRepo;
            this.linksFileRepo = linksFileRepo;
            this.uploadedFilesRepo = uploadedFilesRepo;
            this.uploadedFilesFileRepo = uploadedFilesFileRepo;
        }
    }

    private static final class Services {
        private final EndpointRegistry endpointRegistry;
        private final EndpointsAccessTokensService tokensService;
        private final LinksService linksService;
        private final AuthService authService;
        private final YoutubeService youtubeService;

        private Services(
                EndpointRegistry endpointRegistry,
                EndpointsAccessTokensService tokensService,
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
