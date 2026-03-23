package com.serbekun;


import java.nio.file.Path;

import com.serbekun.service.auth.AuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.config.core.CoreConfig;
import com.serbekun.core.*;
import com.serbekun.http.handles.InitHandles;
// repository
import com.serbekun.repository.*;
import com.serbekun.resources.ResourceCache;
import com.serbekun.resources.ResourceLoader;
// autosave
import com.serbekun.service.autosave.*;
import com.serbekun.service.http.handles.v0.*;
import com.serbekun.service.links.LinksService;
import com.serbekun.service.resource.ResourcesService;
import com.serbekun.service.tokens.EndpointAccessTokensService;
import io.javalin.Javalin;

public class Main {
    
    public static void main(String[] args) {
        
        Logger log = LoggerFactory.getLogger(Main.class);

        // init server folders
        log.info("Init server folder");
        ServerStorageInitializer serverStorageInitializer = new ServerStorageInitializer();
        serverStorageInitializer.initialize(Path.of(CoreConfig.Infrastructure.Fs.getServerStorageFolder()));
        // ==========================

        // init core repository
        log.info("Init core repository");
        LinksRepository linksRepository = new LinksRepository(CoreConfig.LinksConfig.getLinksStorageFile());
        EndpointAccessTokensRepository endpointAccessTokensRepository  = new EndpointAccessTokensRepository(CoreConfig.TokensConfig.getTokensStorageFolder());

        // local tokens repository
        LocalTokensRepository localTokensRepositoryLinks = new LocalTokensRepository(CoreConfig.LinksConfig.getLinksLocalTokensStorageFile());

        // init core
        log.info("Init core");
        Links links = linksRepository.getLinks();
        EndpointsAccessTokens endpointAccessTokens = endpointAccessTokensRepository.getEndpointAccessTokens();


        // init tokens
        LocalTokens localTokensLinks = localTokensRepositoryLinks.getTokens();

        // init services
        log.info("Init services");
        EndpointAccessTokensService EndpointAccessTokensService = new EndpointAccessTokensService(endpointAccessTokens);
        LinksService linksService = new LinksService(links);

        AuthService authService = new AuthService(EndpointAccessTokensService);

        // run auto save threads
        log.info("Init autosave threads");
        
        AutosaveService autosaveService = new AutosaveService();

        autosaveService.register(endpointAccessTokensRepository);
        autosaveService.register(linksRepository);
        
    

        // init server resource
        ResourceLoader loader = new ResourceLoader();
        ResourceCache cache = new ResourceCache(loader);
        ResourcesService resourcesService = new ResourcesService(loader, cache);

        // init javalin
        log.info("Init javalin");
        Javalin svr = Javalin.create();


        // init http handles services
        V0ApiJson v0ApiJson = new V0ApiJson(resourcesService);
        V0ResourcesImages v0ResourcesImages = new V0ResourcesImages(resourcesService);
        V0Page v0Page = new V0Page(resourcesService);
        V0Links V0Links = new V0Links(linksService, localTokensLinks);


        // init api handles
        InitHandles initHandles = new InitHandles();
        initHandles.initHandles(svr, resourcesService, authService,
            v0ApiJson,
            v0ResourcesImages,
            v0Page,
            V0Links
        );

        // shutdown server when JWM is stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Stopping server...");
            svr.stop();
        }));
    
        // run http thread
        Thread httpServerThread = new Thread(() -> {
            svr.start(8080);
        }, "http-server-thread");

        httpServerThread.start();
    }
}
