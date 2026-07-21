package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0RepositoryLinksHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;

import io.javalin.Javalin;

public class RepositoryLinksRoutes implements HttpHandler {

    private final ApiV0RepositoryLinksHttp handler;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0RepositoryLinks = new Endpoint("/api/v0/repository/links");

    public RepositoryLinksRoutes(LinkRepositoryService linkRepositoryService, EndpointRegistrar endpointRegistrar) {
        this.handler = new ApiV0RepositoryLinksHttp(linkRepositoryService);
        this.endpointRegistrar = endpointRegistrar;
    }

    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0RepositoryLinks, false);

        svr.before("/api/v0/repository/links/", ctx -> ctx.attribute("endpoint", endpointApiV0RepositoryLinks));
        svr.before("/api/v0/repository/links/{repositoryId}", ctx -> ctx.attribute("endpoint", endpointApiV0RepositoryLinks));
        svr.before("/api/v0/repository/links/{repositoryId}/links", ctx -> ctx.attribute("endpoint", endpointApiV0RepositoryLinks));
        svr.before("/api/v0/repository/links/{repositoryId}/links/{uuid}", ctx -> ctx.attribute("endpoint", endpointApiV0RepositoryLinks));

        svr.post("/api/v0/repository/links/", ctx -> handler.handle(ctx));
        svr.delete("/api/v0/repository/links/{repositoryId}", ctx -> handler.handle(ctx));
        svr.get("/api/v0/repository/links/{repositoryId}", ctx -> handler.handle(ctx));
        svr.post("/api/v0/repository/links/{repositoryId}/links", ctx -> handler.handle(ctx));
        svr.put("/api/v0/repository/links/{repositoryId}/links/{uuid}", ctx -> handler.handle(ctx));
        svr.delete("/api/v0/repository/links/{repositoryId}/links/{uuid}", ctx -> handler.handle(ctx));
    }
}
