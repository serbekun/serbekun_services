package com.serbekun;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javalin.Javalin;

import com.serbekun.ss.config.Config;
import com.serbekun.ss.config.Paths;
import com.serbekun.ss.http.handles.RouteInitializer;
import com.serbekun.ss.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensFileRepo;
import com.serbekun.ss.repo.endpointaccesstokens.EndpointsAccessTokensRepo;
import com.serbekun.ss.repo.linksrepo.LinkRepositoryFileRepo;
import com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo;
import com.serbekun.ss.repo.shorturl.ShortUrlFileRepo;
import com.serbekun.ss.repo.shorturl.ShortUrlRepo;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesFileRepo;
import com.serbekun.ss.repo.uploadedfiles.UploadedFilesRepo;
import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.EndpointRegistry;
import com.serbekun.ss.service.autosave.*;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.shorturl.ShortUrlService;
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

        // 1. Link Repositories
        LinkRepositoryFileRepo linkRepositoryFileRepo = new LinkRepositoryFileRepo(Paths.LinksRepositoryConfig.getRepositoriesStorageFile());
        LinkRepositoryRepo linkRepositoryRepo = new LinkRepositoryRepo(linkRepositoryFileRepo.load());
        linkRepositoryFileRepo.setReadInterface(linkRepositoryRepo);

        // 2. Endpoint Access Tokens
        EndpointsAccessTokensFileRepo endpointsAccessTokensFileRepo = new EndpointsAccessTokensFileRepo(Paths.TokensConfig.getTokensStorageFolder());
        EndpointsAccessTokensRepo endpointsAccessTokensRepo = new EndpointsAccessTokensRepo(endpointsAccessTokensFileRepo.load());
        endpointsAccessTokensFileRepo.setEndpointsAccessTokensFileRepository(endpointsAccessTokensRepo);

        // 3. Uploaded Files
        UploadedFilesFileRepo uploadedFilesFileRepo = new UploadedFilesFileRepo(Paths.UploadedFilesConfig.getUploadedFilesStorageFile());
        UploadedFilesRepo uploadedFilesRepo = new UploadedFilesRepo(uploadedFilesFileRepo.load());
        uploadedFilesFileRepo.setUploadedFilesReadInterface(uploadedFilesRepo);

        // 4. Short URLs
        ShortUrlFileRepo shortUrlFileRepo = new ShortUrlFileRepo(Paths.ShortUrlConfig.getShortUrlStorageFile());
        ShortUrlRepo shortUrlRepo = new ShortUrlRepo(shortUrlFileRepo.load());
        shortUrlFileRepo.setShortUrlReadInterface(shortUrlRepo);

        return new Repositories(
            linkRepositoryRepo,
            linkRepositoryFileRepo,
            endpointsAccessTokensRepo,
            endpointsAccessTokensFileRepo,
            uploadedFilesRepo,
            uploadedFilesFileRepo,
            shortUrlRepo,
            shortUrlFileRepo
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
        var linkRepositoryService = new LinkRepositoryService(repos.linkRepositoryRepo);
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

        var uploadedFilesCleanupService =
            new UploadedFilesCleanupService(uploadedFilesService, 600);

        // Short URLs
        var shortUrlService = new ShortUrlService(repos.shortUrlRepo);


        return new Services(endpointRegistry, linkRepositoryService, authService,
                youtubeService, uploadedFilesService, uploadedFilesCleanupService,
                shortUrlService);
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

        return new Resources(resourcesService);
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
            services.linkRepositoryService,
            new com.serbekun.ss.service.cipher.CipherService(),
            services.youtubeService,
            services.uploadedFilesService,
            services.shortUrlService
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

        // Force UTF-8 for every response, independent of the server's default
        // JVM charset. ctx.result(String) encodes with responseCharset(), which
        // falls back to Charset.defaultCharset() when no charset is set on the
        // response. On a server running under a C/POSIX locale (or a pre-Java-18
        // JRE) that default is US-ASCII, so non-ASCII characters (─ — → …,
        // cyrillic) get written as '?'. Setting it up front keeps static assets
        // and API JSON correct regardless of the host locale.
        server.before(reqCtx -> reqCtx.res().setCharacterEncoding("UTF-8"));

        RouteInitializer initHandles = new RouteInitializer();
        initHandles.initHandles(
            server,
            ctx.resources.resourcesService,
            ctx.services.authService,
            ctx.services.endpointRegistry,
            ctx.handlers.resourcesService,
            ctx.handlers.linkRepositoryService,
            ctx.handlers.cipherService,
            ctx.handlers.youtubeService,
            ctx.handlers.uploadedFilesService,
            ctx.handlers.shortUrlService
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
        autosave.register(repos.linkRepositoryFileRepo);
        autosave.register(repos.endpointsAccessTokensFileRepo);
        autosave.register(repos.uploadedFilesFileRepo);
        autosave.register(repos.shortUrlFileRepo);
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
        private final LinkRepositoryRepo linkRepositoryRepo;
        private final LinkRepositoryFileRepo linkRepositoryFileRepo;
        private final EndpointsAccessTokensRepo endpointsAccessTokensRepo;
        private final EndpointsAccessTokensFileRepo endpointsAccessTokensFileRepo;
        private final UploadedFilesRepo uploadedFilesRepo;
        private final UploadedFilesFileRepo uploadedFilesFileRepo;
        private final ShortUrlRepo shortUrlRepo;
        private final ShortUrlFileRepo shortUrlFileRepo;

        private Repositories(
                LinkRepositoryRepo linkRepositoryRepo,
                LinkRepositoryFileRepo linkRepositoryFileRepo,
                EndpointsAccessTokensRepo endpointsAccessTokensRepo,
                EndpointsAccessTokensFileRepo endpointsAccessTokensFileRepo,
                UploadedFilesRepo uploadedFilesRepo,
                UploadedFilesFileRepo uploadedFilesFileRepo,
                ShortUrlRepo shortUrlRepo,
                ShortUrlFileRepo shortUrlFileRepo) {
            this.linkRepositoryRepo = linkRepositoryRepo;
            this.linkRepositoryFileRepo = linkRepositoryFileRepo;
            this.endpointsAccessTokensRepo = endpointsAccessTokensRepo;
            this.endpointsAccessTokensFileRepo = endpointsAccessTokensFileRepo;
            this.uploadedFilesRepo = uploadedFilesRepo;
            this.uploadedFilesFileRepo = uploadedFilesFileRepo;
            this.shortUrlRepo = shortUrlRepo;
            this.shortUrlFileRepo = shortUrlFileRepo;
        }
    }

    /**
     * Container class for holding all initialized service instances,
     * including endpoint registry, access tokens, links, authentication,
     * YouTube, and uploaded files services.
     */
    private static final class Services {
        private final EndpointRegistry endpointRegistry;
        private final LinkRepositoryService linkRepositoryService;
        private final AuthService authService;
        private final YoutubeService youtubeService;
        private final UploadedFilesService uploadedFilesService;
        private final UploadedFilesCleanupService uploadedFilesCleanupService;
        private final ShortUrlService shortUrlService;

        private Services(
                EndpointRegistry endpointRegistry,
                LinkRepositoryService linkRepositoryService,
                AuthService authService,
                YoutubeService youtubeService,
                UploadedFilesService uploadedFilesService,
                UploadedFilesCleanupService uploadedFilesCleanupService,
                ShortUrlService shortUrlService) {
            this.endpointRegistry = endpointRegistry;
            this.linkRepositoryService = linkRepositoryService;
            this.authService = authService;
            this.youtubeService = youtubeService;
            this.uploadedFilesService = uploadedFilesService;
            this.uploadedFilesCleanupService = uploadedFilesCleanupService;
            this.shortUrlService = shortUrlService;
        }
    }

    /**
     * Container class for holding all initialized resource instances,
     * including resource loader, cache, and service.
     */
    private static final class Resources {
        private final ResourcesService resourcesService;

        private Resources(
                ResourcesService resourcesService) {
            this.resourcesService = resourcesService;
        }
    }

    private static final class Handlers {
        private final com.serbekun.ss.service.resource.ResourcesService resourcesService;
        private final LinkRepositoryService linkRepositoryService;
        private final com.serbekun.ss.service.cipher.CipherService cipherService;
        private final YoutubeService youtubeService;
        private final UploadedFilesService uploadedFilesService;
        private final ShortUrlService shortUrlService;

        private Handlers(
                com.serbekun.ss.service.resource.ResourcesService resourcesService,
                LinkRepositoryService linkRepositoryService,
                com.serbekun.ss.service.cipher.CipherService cipherService,
                YoutubeService youtubeService,
                UploadedFilesService uploadedFilesService,
                ShortUrlService shortUrlService) {
            this.resourcesService = resourcesService;
            this.linkRepositoryService = linkRepositoryService;
            this.cipherService = cipherService;
            this.youtubeService = youtubeService;
            this.uploadedFilesService = uploadedFilesService;
            this.shortUrlService = shortUrlService;
        }
    }
}
