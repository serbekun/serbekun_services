package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0YoutubeHttp;
import com.serbekun.ss.service.youtube.YoutubeService;

import io.javalin.Javalin;

public class YoutubeRoutes implements HttpHandler {

    private final ApiV0YoutubeHttp apiV0YoutubeHttp;

    public YoutubeRoutes(YoutubeService youtubeService) {
        this.apiV0YoutubeHttp = new ApiV0YoutubeHttp(youtubeService);
    }

    @Override
    public void register(Javalin svr) {
        // No auth endpoints: these routes are intentionally not mapped in the
        // endpoint registry, so the auth gate skips them.
        svr.get("/api/v0/youtube/info", ctx -> apiV0YoutubeHttp.handleInfo(ctx));
        svr.get("/api/v0/youtube/download", ctx -> apiV0YoutubeHttp.handleDownload(ctx));
    }
}
