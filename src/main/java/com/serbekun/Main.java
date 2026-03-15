package com.serbekun;


import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.serbekun.service.auth.AuthService;
import com.serbekun.service.auth.Endpoints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.infrastructure.fs.ServerStorageInitializer;
import com.serbekun.config.core.CoreConfig;
import com.serbekun.core.*;
import com.serbekun.http.handles.InitHandles;
// repository
import com.serbekun.repository.*;
// autosave
import com.serbekun.service.autosave.*;
import com.serbekun.service.tokens.TokensService;
import io.javalin.Javalin;

public class Main {
    
    public static void main(String[] args) {
        
        Logger log = LoggerFactory.getLogger(Main.class);


        // init server folders
        log.info("Init server folder");
        ServerStorageInitializer serverStorageInitializer = new ServerStorageInitializer();
        serverStorageInitializer.initialize(Path.of(CoreConfig.Infrastructure.Fs.getServerStorageFolder()));

        // init core repository
        log.info("Init core repository");
        LinksRepository linksRepository = new LinksRepository(CoreConfig.LinksConfig.getLinksStorageFile());
        TokensRepository tokensRepository = new TokensRepository(CoreConfig.TokensConfig.getTokensStorageFolder());

        // init core
        log.info("Init core");
        Links links = new Links(linksRepository.load()); 
        Tokens tokens = new Tokens(tokensRepository.load());

        // init services
        log.info("Init services");
        TokensService tokensService = new TokensService(tokens);

        // run auto save threads
        log.info("Init autosave threads");
        LinksAutoSaver linksAutoSaver = new LinksAutoSaver(null, linksRepository);
        linksAutoSaver.start();

        TokensAutoSave tokensAutoSave = new TokensAutoSave(tokensService, tokensRepository);
        tokensAutoSave.start();

        List<Endpoints> endpoint_allowed_for_sergei = new ArrayList<>();
        // endpoint_allowed_for_sergei.add(Endpoints.LINKS);

        tokensService.addToken("sergei", endpoint_allowed_for_sergei);

        // init javalin
        log.info("Init javalin");
        Javalin svr = Javalin.create();

        // init api handles
        InitHandles initHandles = new InitHandles();
        initHandles.initHandles(svr);

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
