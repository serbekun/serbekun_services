package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0IpInfoHttp;

import io.javalin.Javalin;

public class NetworkRoutes implements HttpHandler {

    private final ApiV0IpInfoHttp apiV0IpInfoHttp;

    public NetworkRoutes() {
        this.apiV0IpInfoHttp = new ApiV0IpInfoHttp();
    }

    @Override
    public void register(Javalin svr) {
        // No auth endpoints: this route is intentionally not mapped in the
        // endpoint registry, so the auth gate skips it.
        svr.get("/api/v0/network/ip", ctx -> apiV0IpInfoHttp.handle(ctx));
    }

}
