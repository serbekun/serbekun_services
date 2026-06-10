package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

/**
 * Aggregator for API v0 routes.
 */
public class ApiV0Routes {

    private final CipherRoutes cipherRoutes;
    private final LinkCatalogRoutes linkCatalogRoutes;
    private final YoutubeRoutes youtubeRoutes;

    public ApiV0Routes(CipherRoutes cipherRoutes, LinkCatalogRoutes linkCatalogRoutes, YoutubeRoutes youtubeRoutes) {
        this.cipherRoutes = cipherRoutes;
        this.linkCatalogRoutes = linkCatalogRoutes;
        this.youtubeRoutes = youtubeRoutes;
    }

    /**
     * Registers all API v0 routes.
     */
    public void register(Javalin svr) {
        cipherRoutes.register(svr);
        linkCatalogRoutes.register(svr);
        youtubeRoutes.register(svr);
    }
}
