package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

/**
 * Aggregator for API v0 routes.
 */
public class ApiV0Routes {

    private final CipherRoutes cipherRoutes;
    private final LinkCatalogRoutes linkCatalogRoutes;
    private final YoutubeRoutes youtubeRoutes;
    private final UploadedFilesRoutes uploadedFilesRoutes;

    public ApiV0Routes(CipherRoutes cipherRoutes, LinkCatalogRoutes linkCatalogRoutes, YoutubeRoutes youtubeRoutes,
                       UploadedFilesRoutes uploadedFilesRoutes) {
        this.cipherRoutes = cipherRoutes;
        this.linkCatalogRoutes = linkCatalogRoutes;
        this.youtubeRoutes = youtubeRoutes;
        this.uploadedFilesRoutes = uploadedFilesRoutes;
    }

    /**
     * Registers all API v0 routes.
     */
    public void register(Javalin svr) {
        cipherRoutes.register(svr);
        linkCatalogRoutes.register(svr);
        youtubeRoutes.register(svr);
        uploadedFilesRoutes.register(svr);
    }
}
