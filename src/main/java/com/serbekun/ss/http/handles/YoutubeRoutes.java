package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0YoutubeHttp;

import io.javalin.Javalin;

public class YoutubeRoutes {

    private final ApiV0YoutubeHttp apiV0YoutubeHttp;

    public YoutubeRoutes(ApiV0YoutubeHttp apiV0YoutubeHttp) {
        this.apiV0YoutubeHttp = apiV0YoutubeHttp;
    }

    public void register(Javalin svr) {
        svr.get("/api/v0/youtube/info", ctx -> apiV0YoutubeHttp.handleInfo(ctx));
        svr.get("/api/v0/youtube/download", ctx -> apiV0YoutubeHttp.handleDownload(ctx));
    }
}
