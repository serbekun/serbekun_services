package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0JsonHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.json.JsonService;

import io.javalin.Javalin;

/**
 * Routes for working with JSON documents.
 */
public class JsonRoutes implements HttpHandler {

    private static final String[] OPERATIONS = {"validate", "format", "minify", "query", "diff"};

    private final ApiV0JsonHttp apiV0JsonHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Json = new Endpoint("/api/v0/json");

    public JsonRoutes(JsonService jsonService, EndpointRegistrar endpointRegistrar) {
        this.apiV0JsonHttp = new ApiV0JsonHttp(jsonService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers JSON routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Json, false);

        for (String operation : OPERATIONS) {
            String path = "/api/v0/json/" + operation;
            svr.before(path, ctx -> ctx.attribute("endpoint", endpointApiV0Json));
            svr.post(path, ctx -> apiV0JsonHttp.main(ctx));
        }
    }
}
