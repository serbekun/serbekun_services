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
        StaticV0Json v0ApiJson,
        StaticV0Images v0ResourcesImages,
        StaticV0Html v0Page,
        ApiV0CatalogsLinks v0Links,
        ApiV0CipherAes v0ApiCipherAes

    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        V0ImagesHttp Images = new V0ImagesHttp(resourcesService, v0ResourcesImages);
        V0JsonHttp json = new V0JsonHttp(v0ApiJson);
        V0CipherAesHttp cipherAes = new V0CipherAesHttp(v0ApiCipherAes);
        V0PagesHttp pages = new V0PagesHttp(v0Page);
        V0LinksHttp links = new V0LinksHttp(v0Links);

        // SINGLE instances
        Endpoint endpointIndex = new Endpoint("Index");
        Endpoint endpointV0StaticImages = new Endpoint("v0StaticImages");
        Endpoint endpointV0ApiJson = new Endpoint("v0ApiJson");
        Endpoint endpointV0ApiCipherAes = new Endpoint("v0ApiCipherAes");
        Endpoint endpointV0Page = new Endpoint("v0Page");

        // register
        endpointRegistrar.register(endpointIndex, false);
        endpointRegistrar.register(endpointV0StaticImages, false);
        endpointRegistrar.register(endpointV0ApiJson, false);
        endpointRegistrar.register(endpointV0ApiCipherAes, false);
        endpointRegistrar.register(endpointV0Page, false);

        svr.before("/", ctx -> ctx.attribute("endpoint", endpointIndex));
        svr.before("/static/v0/images/{name}", ctx -> ctx.attribute("endpoint", endpointV0StaticImages));
        svr.before("/static/v0/json/{name}", ctx -> ctx.attribute("endpoint", endpointV0ApiJson));
        svr.before("/api/v0/cipher/aes", ctx -> ctx.attribute("endpoint", endpointV0ApiCipherAes));
        svr.before("/api/v0/cipher/aes/encrypt", ctx -> ctx.attribute("endpoint", endpointV0ApiCipherAes));
        svr.before("/api/v0/cipher/aes/decrypt", ctx -> ctx.attribute("endpoint", endpointV0ApiCipherAes));
        svr.before("/static/v0/html/{name}", ctx -> ctx.attribute("endpoint", endpointV0Page));

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
        svr.get("/static/v0/images/{name}", ctx -> Images.main(ctx));
        svr.get("/static/v0/json/{name}", ctx -> json.main(ctx));
        svr.get("/static/v0/html/{name}", ctx -> pages.main(ctx));

        
        svr.get("/api/v0/cipher/aes", ctx -> cipherAes.main(ctx));
        svr.post("/api/v0/cipher/aes/encrypt", ctx -> cipherAes.main(ctx));
        svr.post("/api/v0/cipher/aes/decrypt", ctx -> cipherAes.main(ctx));

        // in future
        // /api/v0/cipher/rsa/*

        // links
        svr.get("/api/v0/catalogs/links", ctx -> links.main(ctx));
        svr.post("/api/v0/catalogs/links", ctx -> links.main(ctx));
        svr.put("/api/v0/catalogs/links/{uuid}", ctx -> links.main(ctx));
        svr.delete("/api/v0/catalogs/links/{uuid}", ctx -> links.main(ctx));
        // in future programs поэтому catlogs чтоб можно было новые каталоги добавить
    } 
}
