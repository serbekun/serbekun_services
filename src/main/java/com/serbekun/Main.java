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
        EndpointRegistry endpointRegistry = new EndpointRegistry();
        EndpointAccessTokensService EndpointAccessTokensService = new EndpointAccessTokensService(endpointAccessTokens);
        LinksService linksService = new LinksService(links);

        AuthService authService = new AuthService(EndpointAccessTokensService, endpointRegistry);

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
        V0ApiCipherAes v0ApiCipherAes = new V0ApiCipherAes();
        

        // init api handles
        InitHandles initHandles = new InitHandles();
        initHandles.initHandles(svr, resourcesService, authService, endpointRegistry,
            v0ApiJson,
            v0ResourcesImages,
            v0Page,
            V0Links,
            v0ApiCipherAes
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

        autosaveService.start();
    }
}
