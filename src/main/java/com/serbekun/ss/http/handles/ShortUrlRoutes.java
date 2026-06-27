package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0ShortUrlHttp;

import io.javalin.Javalin;

/**
 * Routes for working with shortened URLs.
 */
public class ShortUrlRoutes {

    private final ApiV0ShortUrlHttp apiV0ShortUrlHttp;

    public ShortUrlRoutes(ApiV0ShortUrlHttp apiV0ShortUrlHttp) {
        this.apiV0ShortUrlHttp = apiV0ShortUrlHttp;
    }

    /**
     * Registers short url routes.
     */
    public void register(Javalin svr) {
        svr.get("/api/v0/short-url/{id}", ctx -> apiV0ShortUrlHttp.main(ctx));
        svr.post("/api/v0/short-url", ctx -> apiV0ShortUrlHttp.main(ctx));
        svr.delete("/api/v0/short-url/{id}", ctx -> apiV0ShortUrlHttp.main(ctx));
    }
}
