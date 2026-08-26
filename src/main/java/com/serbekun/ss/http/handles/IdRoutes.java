package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0IdHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.id.IdService;

import io.javalin.Javalin;

/**
 * Routes for identifiers and random material.
 * <p>
 * Two endpoints rather than one: {@code /api/v0/id} issues identifiers meant to
 * be shared, {@code /api/v0/random} issues secrets. They are the same generator
 * underneath, but keeping them apart means the day one of them needs a token or
 * a rate limit, it can have one without the other.
 */
public class IdRoutes implements HttpHandler {

    private final ApiV0IdHttp apiV0IdHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Id = new Endpoint("/api/v0/id");
    private final Endpoint endpointApiV0Random = new Endpoint("/api/v0/random");

    public IdRoutes(IdService idService, EndpointRegistrar endpointRegistrar) {
        this.apiV0IdHttp = new ApiV0IdHttp(idService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers identifier routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Id, false);
        endpointRegistrar.register(endpointApiV0Random, false);

        for (String path : new String[] {"/api/v0/id/uuid", "/api/v0/id/ulid", "/api/v0/id/batch"}) {
            svr.before(path, ctx -> ctx.attribute("endpoint", endpointApiV0Id));
        }
        for (String path : new String[] {"/api/v0/random/token", "/api/v0/random/bytes"}) {
            svr.before(path, ctx -> ctx.attribute("endpoint", endpointApiV0Random));
        }

        svr.get("/api/v0/id/uuid", ctx -> apiV0IdHttp.main(ctx));
        svr.get("/api/v0/id/ulid", ctx -> apiV0IdHttp.main(ctx));
        svr.post("/api/v0/id/batch", ctx -> apiV0IdHttp.main(ctx));

        svr.get("/api/v0/random/token", ctx -> apiV0IdHttp.main(ctx));
        svr.get("/api/v0/random/bytes", ctx -> apiV0IdHttp.main(ctx));
    }
}
