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
import com.serbekun.ss.service.uploadedfiles.UploadedFilesCleanupService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.Youtube;
import com.serbekun.ss.service.youtube.YoutubeDomains;
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

    /**
     * Loads the server configuration from the specified config file.
     * @return Loaded Config object
     * @throws RuntimeException if the config file cannot be loaded
     */
    private static Config loadConfig() {
        log.info("Loading server config");
        return Config.load(Path.of(Paths.Infrastructure.Fs.getServerStorageFolder(), "config.json"));
    }

    /**
     * Initializes the application by setting up storage, repositories, services, resources, and HTTP handlers.
     * @param config The server configuration
     * @return ServerContext containing initialized components
     * @throws RuntimeException if any component fails to initialize
     */
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

    /**
     * Initializes the server storage folders.
     */
    private static void initializeStorage() {
        log.info("Initializing server storage folders");
        new ServerStorageInitializer()
            .initialize(Path.of(Paths.Infrastructure.Fs.getServerStorageFolder()));
    }

    /**
     * Initializes the repositories for links, endpoint access tokens, local tokens, and uploaded files.
     * @return Repositories object containing all initialized repositories
     */
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

    /**
     * Initializes the services for endpoint registry, access tokens, links, authentication, YouTube, and uploaded files.
     * @param repos The repositories containing data for services
     * @param config The server configuration
     * @return Services object containing all initialized services
     * @throws RuntimeException if any service fails to initialize
     */
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
        var youtubeDomains = new YoutubeDomains(new ResourceLoader());
        var youtubeService = new YoutubeService(youtube, youtubeDomains);

        // Uploaded Files
        var uploadedFilesService = new UploadedFilesService(
                repos.uploadedFilesRepo,
                Paths.UploadedFilesConfig.getUploadedFilesRAWFolder());
        var uploadedFilesCleanupService = new UploadedFilesCleanupService(uploadedFilesService, 60);

        return new Services(endpointRegistry, endpointAccessTokensService, linksService, authService,
                youtubeService, uploadedFilesService, uploadedFilesCleanupService);
    }

    /**
     * Initializes the resources for the application, including resource loader, cache, and service.
     * @return Resources object containing all initialized resources
     */
    private static Resources initializeResources() {
        log.info("Initializing resources");
        var resourceLoader = new ResourceLoader();
        var resourceCache = new ResourceCache(resourceLoader);
        var resourcesService = new ResourcesService(resourceLoader, resourceCache);

        return new Resources(resourceLoader, resourceCache, resourcesService);
    }

    /**
     * Initializes the HTTP handlers for the application, including resources, links, cipher, YouTube, and uploaded files.
     * @param services The services containing business logic for the application
     * @param resources The resources containing resource management for the application
     * @param repos The repositories containing data for the application
     * @return Handlers object containing all initialized HTTP handlers
     * @throws RuntimeException if any handler fails to initialize
     */
    private static Handlers initializeHandlers(Services services, Resources resources, Repositories repos) {
        log.info("Initializing HTTP handlers");

        return new Handlers(
            resources.resourcesService,
            services.linksService,
            new com.serbekun.ss.service.cipher.CipherService(),
            services.youtubeService,
            services.uploadedFilesService
        );
    }

    /**
     * Starts the Javalin server with the provided context and configuration.
     * @param ctx The server context containing initialized components
     * @param config The server configuration
     * @throws RuntimeException if the server fails to start 
    */
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
            ctx.handlers.youtubeService,
            ctx.handlers.uploadedFilesService
        );

        // Autosave
        AutosaveService autosaveService = createAndStartAutosave(ctx.repos);

        // Uploaded files expiration cleanup
        ctx.services.uploadedFilesCleanupService.start();

        addShutdownHook(server, autosaveService, ctx.services.uploadedFilesCleanupService);

        // Run
        server.start(config.getPort());
    }

    /**
     * Creates and starts the autosave service, registering the necessary repositories for periodic saving.
     * @param repos The repositories to be registered with the autosave service
     * @return The initialized and started AutosaveService
     */
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

    /**
     * Adds a shutdown hook to gracefully stop the server, autosave service, and uploaded files cleanup service on application termination.
     * @param server The Javalin server instance to be stopped
     * @param autosave The AutosaveService instance to be stopped
     * @param cleanupService The UploadedFilesCleanupService instance to be stopped
     */
    private static void addShutdownHook(Javalin server, AutosaveService autosave,
                                         UploadedFilesCleanupService cleanupService) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            autosave.stop();
            cleanupService.stop();
            server.stop();
        }, "shutdown-hook"));
    }

    /**
     * Container class for holding all initialized components of the server, including repositories, services, resources, and handlers.
     */
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

    /**
     * Container class for holding all initialized repository instances.
     */
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

    /**
     * Container class for holding all initialized service instances,
     * including endpoint registry, access tokens, links, authentication,
     * YouTube, and uploaded files services.
     */
    private static final class Services {
        private final EndpointRegistry endpointRegistry;
        private final EndpointsAccessTokensService tokensService;
        private final LinksService linksService;
        private final AuthService authService;
        private final YoutubeService youtubeService;
        private final UploadedFilesService uploadedFilesService;
        private final UploadedFilesCleanupService uploadedFilesCleanupService;

        private Services(
                EndpointRegistry endpointRegistry,
                EndpointsAccessTokensService tokensService,
                LinksService linksService,
                AuthService authService,
                YoutubeService youtubeService,
                UploadedFilesService uploadedFilesService,
                UploadedFilesCleanupService uploadedFilesCleanupService) {
            this.endpointRegistry = endpointRegistry;
            this.tokensService = tokensService;
            this.linksService = linksService;
            this.authService = authService;
            this.youtubeService = youtubeService;
            this.uploadedFilesService = uploadedFilesService;
            this.uploadedFilesCleanupService = uploadedFilesCleanupService;
        }
    }

    /**
     * Container class for holding all initialized resource instances,
     * including resource loader, cache, and service.
     */
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
        private final UploadedFilesService uploadedFilesService;

        private Handlers(
                com.serbekun.ss.service.resource.ResourcesService resourcesService,
                com.serbekun.ss.service.links.LinksService linksService,
                com.serbekun.ss.service.cipher.CipherService cipherService,
                YoutubeService youtubeService,
                UploadedFilesService uploadedFilesService) {
            this.resourcesService = resourcesService;
            this.linksService = linksService;
            this.cipherService = cipherService;
            this.youtubeService = youtubeService;
            this.uploadedFilesService = uploadedFilesService;
        }
    }
}
