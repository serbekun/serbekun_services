package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

public class ApiV0Routes {

    private final CipherRoutes cipherRoutes;
    private final RepositoryLinksRoutes repositoryLinksRoutes;
    private final YoutubeRoutes youtubeRoutes;
    private final UploadedFilesRoutes uploadedFilesRoutes;
    private final ShortUrlRoutes shortUrlRoutes;
    private final VersionRoutes versionRoutes;

    public ApiV0Routes(CipherRoutes cipherRoutes, RepositoryLinksRoutes repositoryLinksRoutes, YoutubeRoutes youtubeRoutes,
                       UploadedFilesRoutes uploadedFilesRoutes, ShortUrlRoutes shortUrlRoutes, VersionRoutes versionRoutes) {
        this.cipherRoutes = cipherRoutes;
        this.repositoryLinksRoutes = repositoryLinksRoutes;
        this.youtubeRoutes = youtubeRoutes;
        this.uploadedFilesRoutes = uploadedFilesRoutes;
        this.shortUrlRoutes = shortUrlRoutes;
        this.versionRoutes = versionRoutes;
    }

    public void register(Javalin svr) {
        cipherRoutes.register(svr);
        repositoryLinksRoutes.register(svr);
        youtubeRoutes.register(svr);
        uploadedFilesRoutes.register(svr);
        shortUrlRoutes.register(svr);
        versionRoutes.register(svr);
    }
}
