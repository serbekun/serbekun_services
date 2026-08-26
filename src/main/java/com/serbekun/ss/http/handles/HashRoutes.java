package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0HashHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.hash.HashService;

import io.javalin.Javalin;

/**
 * Routes for hashing and integrity checking.
 */
public class HashRoutes implements HttpHandler {

    private final ApiV0HashHttp apiV0HashHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Hash = new Endpoint("/api/v0/hash");

    public HashRoutes(HashService hashService, EndpointRegistrar endpointRegistrar) {
        this.apiV0HashHttp = new ApiV0HashHttp(hashService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers hash routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Hash, false);

        svr.before("/api/v0/hash", ctx -> ctx.attribute("endpoint", endpointApiV0Hash));
        svr.before("/api/v0/hash/file", ctx -> ctx.attribute("endpoint", endpointApiV0Hash));
        svr.before("/api/v0/hash/verify", ctx -> ctx.attribute("endpoint", endpointApiV0Hash));

        svr.post("/api/v0/hash", ctx -> apiV0HashHttp.main(ctx));
        svr.post("/api/v0/hash/file", ctx -> apiV0HashHttp.main(ctx));
        svr.post("/api/v0/hash/verify", ctx -> apiV0HashHttp.main(ctx));
    }
}
