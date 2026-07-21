package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.api.ApiVersion;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;

public class VersionRoutes implements HttpHandler {

    private final ApiVersion apiVersion;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Version = new Endpoint("/api/v0/version");

    public VersionRoutes(EndpointRegistrar endpointRegistrar) {
        this.apiVersion = new ApiVersion();
        this.endpointRegistrar = endpointRegistrar;
    }

    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Version, false);

        svr.before("/api/v0/version", ctx -> ctx.attribute("endpoint", endpointApiV0Version));

        svr.get("/api/v0/version", ctx -> apiVersion.main(ctx));
    }
}
