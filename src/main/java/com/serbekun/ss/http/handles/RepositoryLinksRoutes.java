package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0RepositoryLinksHttp;

import io.javalin.Javalin;

public class RepositoryLinksRoutes {

    private final ApiV0RepositoryLinksHttp handler;

    public RepositoryLinksRoutes(ApiV0RepositoryLinksHttp handler) {
        this.handler = handler;
    }

    public void register(Javalin svr) {
        svr.post("/api/v0/repository/links/", ctx -> handler.handle(ctx));
        svr.delete("/api/v0/repository/links/{repositoryId}", ctx -> handler.handle(ctx));
        svr.get("/api/v0/repository/links/{repositoryId}", ctx -> handler.handle(ctx));
        svr.post("/api/v0/repository/links/{repositoryId}/links", ctx -> handler.handle(ctx));
        svr.put("/api/v0/repository/links/{repositoryId}/links/{uuid}", ctx -> handler.handle(ctx));
        svr.delete("/api/v0/repository/links/{repositoryId}/links/{uuid}", ctx -> handler.handle(ctx));
    }
}
