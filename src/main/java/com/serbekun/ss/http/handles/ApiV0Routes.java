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
    private final ShortUrlRoutes shortUrlRoutes;
    private final VersionRoutes versionRoutes;

    public ApiV0Routes(CipherRoutes cipherRoutes, LinkCatalogRoutes linkCatalogRoutes, YoutubeRoutes youtubeRoutes,
                       UploadedFilesRoutes uploadedFilesRoutes, ShortUrlRoutes shortUrlRoutes, VersionRoutes versionRoutes) {
        this.cipherRoutes = cipherRoutes;
        this.linkCatalogRoutes = linkCatalogRoutes;
        this.youtubeRoutes = youtubeRoutes;
        this.uploadedFilesRoutes = uploadedFilesRoutes;
        this.shortUrlRoutes = shortUrlRoutes;
        this.versionRoutes = versionRoutes;
    }

    /**
     * Registers all API v0 routes.
     */
    public void register(Javalin svr) {
        cipherRoutes.register(svr);
        linkCatalogRoutes.register(svr);
        youtubeRoutes.register(svr);
        uploadedFilesRoutes.register(svr);
        shortUrlRoutes.register(svr);
        versionRoutes.register(svr);
    }
}
