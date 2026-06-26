package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0CatalogsLinksHttp;

import io.javalin.Javalin;

/**
 * Routes for working with link catalog.
 */
public class LinkCatalogRoutes {

    private final ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp;

    public LinkCatalogRoutes(ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp) {
        this.apiV0CatalogsLinksHttp = apiV0CatalogsLinksHttp;
    }

    /**
     * Registers link catalog routes.
     */
    public void register(Javalin svr) {
        svr.get("/api/v0/catalogs/links", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.post("/api/v0/catalogs/links", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.put("/api/v0/catalogs/links/{uuid}", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.delete("/api/v0/catalogs/links/{uuid}", ctx -> apiV0CatalogsLinksHttp.main(ctx));
    }
}
