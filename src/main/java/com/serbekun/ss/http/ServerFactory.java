package com.serbekun.ss.http;

import java.util.List;

import io.javalin.Javalin;
import io.javalin.config.SizeUnit;

import com.serbekun.ss.config.Config;
import com.serbekun.ss.http.handles.CipherRoutes;
import com.serbekun.ss.http.handles.HttpHandler;
import com.serbekun.ss.http.handles.NetworkRoutes;
import com.serbekun.ss.http.handles.RepositoryLinksRoutes;
import com.serbekun.ss.http.handles.ShortUrlRoutes;
import com.serbekun.ss.http.handles.StaticRoutes;
import com.serbekun.ss.http.handles.UploadedFilesRoutes;
import com.serbekun.ss.http.handles.VersionRoutes;
import com.serbekun.ss.http.handles.YoutubeRoutes;
import com.serbekun.ss.http.middleware.AuthMiddleware;
import com.serbekun.ss.http.middleware.CharsetMiddleware;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.shorturl.ShortUrlService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.YoutubeService;

/**
 * App factory: builds a Javalin instance with all middleware and handlers
 * registered. The single authoritative list of handlers lives here — to add
 * a new endpoint, create a class implementing {@link HttpHandler} and add it
 * to the list below, nothing else. Reused by Main and the integration tests
 * so they never drift apart.
 */
public final class ServerFactory {

    private ServerFactory() {
    }

    public static Javalin create(Config config,
            ResourcesService resourcesService,
            LinkRepositoryService linkRepositoryService,
            CipherService cipherService,
            YoutubeService youtubeService,
            UploadedFilesService uploadedFilesService,
            ShortUrlService shortUrlService,
            AuthService authService,
            EndpointRegistrar endpointRegistrar) {

        Javalin server = Javalin.create(javalinConfig -> {
            javalinConfig.http.maxRequestSize = config.getUploadFileMaxSizeBytes() + 1024L * 1024L;
            javalinConfig.jetty.multipartConfig.maxFileSize(config.getUploadFileMaxSizeMb(), SizeUnit.MB);
        });

        List<HttpHandler> handlers = List.of(
            new StaticRoutes(resourcesService, endpointRegistrar),
            new CipherRoutes(cipherService, endpointRegistrar),
            new RepositoryLinksRoutes(linkRepositoryService, endpointRegistrar),
            new YoutubeRoutes(youtubeService),
            new UploadedFilesRoutes(uploadedFilesService, config, endpointRegistrar),
            new ShortUrlRoutes(shortUrlService, endpointRegistrar),
            new VersionRoutes(endpointRegistrar),
            new NetworkRoutes()
        );

        InitHttp initHttp = new InitHttp(new CharsetMiddleware(), handlers, new AuthMiddleware(authService));
        initHttp.register(server);

        return server;
    }
}
