package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0ShortUrlHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.shorturl.ShortUrlService;

import io.javalin.Javalin;

/**
 * Routes for working with shortened URLs.
 */
public class ShortUrlRoutes implements HttpHandler {

    private final ApiV0ShortUrlHttp apiV0ShortUrlHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0ShortUrl = new Endpoint("/api/v0/short-url");

    public ShortUrlRoutes(ShortUrlService shortUrlService, EndpointRegistrar endpointRegistrar) {
        this.apiV0ShortUrlHttp = new ApiV0ShortUrlHttp(shortUrlService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers short url routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0ShortUrl, false);

        svr.before("/api/v0/short-url", ctx -> ctx.attribute("endpoint", endpointApiV0ShortUrl));
        svr.before("/api/v0/short-url/{id}", ctx -> ctx.attribute("endpoint", endpointApiV0ShortUrl));

        svr.get("/api/v0/short-url/{id}", ctx -> apiV0ShortUrlHttp.main(ctx));
        svr.post("/api/v0/short-url", ctx -> apiV0ShortUrlHttp.main(ctx));
        svr.delete("/api/v0/short-url/{id}", ctx -> apiV0ShortUrlHttp.main(ctx));
    }
}
