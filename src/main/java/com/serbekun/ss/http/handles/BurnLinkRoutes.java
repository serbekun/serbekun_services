package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0BurnLinkHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.burnlink.BurnLinkService;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.Javalin;

/**
 * Routes for self-destructing burn links.
 * <p>
 * The public link lives under {@code /b/{id}} (page) and
 * {@code /api/v0/burn/{id}/reveal} (burn); management lives under
 * {@code /api/v0/burn}.
 */
public class BurnLinkRoutes implements HttpHandler {

    private final ApiV0BurnLinkHttp apiV0BurnLinkHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Burn = new Endpoint("/api/v0/burn");

    public BurnLinkRoutes(BurnLinkService burnLinkService, ResourcesService resourcesService,
            EndpointRegistrar endpointRegistrar) {
        this.apiV0BurnLinkHttp = new ApiV0BurnLinkHttp(burnLinkService, resourcesService);
        this.endpointRegistrar = endpointRegistrar;
    }

    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Burn, false);

        svr.before("/api/v0/burn", ctx -> ctx.attribute("endpoint", endpointApiV0Burn));
        svr.before("/api/v0/burn/{id}", ctx -> ctx.attribute("endpoint", endpointApiV0Burn));
        svr.before("/api/v0/burn/{id}/reveal", ctx -> ctx.attribute("endpoint", endpointApiV0Burn));

        svr.post("/api/v0/burn", ctx -> apiV0BurnLinkHttp.handleCreate(ctx));
        svr.post("/api/v0/burn/{id}/reveal", ctx -> apiV0BurnLinkHttp.handleReveal(ctx));
        svr.delete("/api/v0/burn/{id}", ctx -> apiV0BurnLinkHttp.handleDelete(ctx));
        svr.get("/b/{id}", ctx -> apiV0BurnLinkHttp.handlePage(ctx));
    }
}
