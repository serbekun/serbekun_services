package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.v0.*;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.*;
import com.serbekun.ss.service.http.handles.v0.*;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.http.UnauthorizedResponse;
import io.javalin.Javalin;

public class InitHandles {

    public void initHandles(Javalin svr, ResourcesService resourcesService,
        AuthService authService, EndpointRegistrar endpointRegistrar,

        // API handles services
        StaticV0Json staticV0Json,
        StaticV0Images staticV0Images,
        StaticV0Html staticV0Html,
        ApiV0CatalogsLinks apiV0CatalogsLinks,
        ApiV0CipherAes apiV0CipherAes

    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        StaticV0ImagesHttp staticV0ImagesHttp = new StaticV0ImagesHttp(resourcesService, staticV0Images);
        StaticV0JsonHttp staticV0JsonHttp = new StaticV0JsonHttp(staticV0Json);
        StaticV0HtmlHttp staticV0HtmlHttp = new StaticV0HtmlHttp(staticV0Html);
        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(apiV0CipherAes);
        ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp = new ApiV0CatalogsLinksHttp(apiV0CatalogsLinks);

        // SINGLE instances
        Endpoint endpointIndex = new Endpoint("/index");
        Endpoint endpointStaticV0Images = new Endpoint("/static/v0/images");
        Endpoint endpointStaticV0Json = new Endpoint("/static/v0/json/");
        Endpoint endpointStaticV0Html = new Endpoint("/static/v0/html/");
        Endpoint endpointApiV0CipherAes = new Endpoint("/api/v0/cipher");
        Endpoint endpointApiV0CatalogsLinks = new Endpoint("/api/v0/catalogs/links");

        // register
        endpointRegistrar.register(endpointIndex, false);
        endpointRegistrar.register(endpointStaticV0Images, false);
        endpointRegistrar.register(endpointStaticV0Json, false);
        endpointRegistrar.register(endpointStaticV0Html, false);
        endpointRegistrar.register(endpointApiV0CipherAes, false);
        endpointRegistrar.register(endpointApiV0CatalogsLinks, false);

        svr.before("/", ctx -> ctx.attribute("endpoint", endpointIndex));
        svr.before("/static/v0/images/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Images));
        svr.before("/static/v0/json/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/html/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Html));
        svr.before("/api/v0/cipher/aes", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/aes/encrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/aes/decrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/catalogs/links", ctx -> ctx.attribute("endpoint", endpointApiV0CatalogsLinks));
        svr.before("/api/v0/catalogs/links/{uuid}", ctx -> ctx.attribute("endpoint", endpointApiV0CatalogsLinks));

        // auth gate
        svr.before(ctx -> {
            Endpoint endpoint = ctx.attribute("endpoint");
            
            if (endpoint == null) {
                return;
            }

            String token = null;

            // Authorization header
            String authHeader = ctx.header("Authorization");
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7).trim();
                } else {
                    token = authHeader.trim();
                }
            }

            // query ?token=
            if (token == null) {
                token = ctx.queryParam("token");
            }

            // query ?Authorization=Bearer ...
            if (token == null) {
                String authInQuery = ctx.queryParam("Authorization");
                if (authInQuery != null && authInQuery.startsWith("Bearer ")) {
                    token = authInQuery.substring(7).trim();
                }
            }

            boolean authorized = authService.checkAuth(endpoint, token);
            if (!authorized) {
                if (token == null || token.isBlank()) {
                    throw new UnauthorizedResponse("Missing or invalid token");
                }
                throw new UnauthorizedResponse("Unauthorized");
            }

            if (token != null && !token.isBlank()) {
                ctx.attribute("userToken", token);
            }
        });

        // routes
        svr.get("/", ctx -> index.main(ctx));

        // static
        svr.get("/static/v0/images/{name}", ctx -> staticV0ImagesHttp.main(ctx));
        svr.get("/static/v0/json/{name}", ctx -> staticV0JsonHttp.main(ctx));
        svr.get("/static/v0/html/{name}", ctx -> staticV0HtmlHttp.main(ctx));

        
        svr.get("/api/v0/cipher/aes", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/encrypt", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/decrypt", ctx -> apiV0CipherAesHttp.main(ctx));

        // in future
        // /api/v0/cipher/rsa/*

        // links
        svr.get("/api/v0/catalogs/links", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.post("/api/v0/catalogs/links", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.put("/api/v0/catalogs/links/{uuid}", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.delete("/api/v0/catalogs/links/{uuid}", ctx -> apiV0CatalogsLinksHttp.main(ctx));
    } 
}
