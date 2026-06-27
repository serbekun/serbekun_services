package com.serbekun.ss.http.handles;

import io.javalin.http.UnauthorizedResponse;
import io.javalin.Javalin;

import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.auth.AuthService;

public class EndpointAuthInitializer {
 
    public static void initHandlesAuthSetting(Javalin svr, EndpointRegistrar endpointRegistrar, AuthService authService) {
        
                // SINGLE instances
        Endpoint endpointIndex = new Endpoint("/index");
        Endpoint endpointStaticV0Images = new Endpoint("/static/v0/images");
        Endpoint endpointStaticV0Json = new Endpoint("/static/v0/json");
        Endpoint endpointStaticV0Html = new Endpoint("/static/v0/html/");
        Endpoint endpointApiV0CipherAes = new Endpoint("/api/v0/cipher");
        Endpoint endpointApiV0CatalogsLinks = new Endpoint("/api/v0/catalogs/links");
        Endpoint endpointApiV0ShortUrl = new Endpoint("/api/v0/short-url");
        Endpoint endpointApiV0Version = new Endpoint("/api/v0/version");

        // register
        endpointRegistrar.register(endpointIndex, false);
        endpointRegistrar.register(endpointStaticV0Images, false);
        endpointRegistrar.register(endpointStaticV0Json, false);
        endpointRegistrar.register(endpointStaticV0Html, false);
        endpointRegistrar.register(endpointApiV0CipherAes, false);
        endpointRegistrar.register(endpointApiV0CatalogsLinks, false);
        endpointRegistrar.register(endpointApiV0ShortUrl, false);
        endpointRegistrar.register(endpointApiV0Version, false);

        svr.before("/", ctx -> ctx.attribute("endpoint", endpointIndex));
        svr.before("/static/v0/images/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Images));
        svr.before("/static/v0/json", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/json/", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/json/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/html/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Html));
        svr.before("/api/v0/cipher/aes", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/aes/encrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/aes/decrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/catalogs/links", ctx -> ctx.attribute("endpoint", endpointApiV0CatalogsLinks));
        svr.before("/api/v0/catalogs/links/{uuid}", ctx -> ctx.attribute("endpoint", endpointApiV0CatalogsLinks));
        svr.before("/api/v0/short-url", ctx -> ctx.attribute("endpoint", endpointApiV0ShortUrl));
        svr.before("/api/v0/short-url/{id}", ctx -> ctx.attribute("endpoint", endpointApiV0ShortUrl));
        svr.before("/api/v0/version", ctx -> ctx.attribute("endpoint", endpointApiV0Version));

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
    }
}
