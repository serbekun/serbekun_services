package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0IpInfoHttp;

import io.javalin.Javalin;

public class NetworkRoutes {

    private final ApiV0IpInfoHttp apiV0IpInfoHttp;

    public NetworkRoutes(ApiV0IpInfoHttp apiV0IpInfoHttp) {
        this.apiV0IpInfoHttp = apiV0IpInfoHttp;
    }

    public void register(Javalin svr) {
        svr.get("/api/v0/network/ip", ctx -> apiV0IpInfoHttp.handle(ctx));
    }

}
