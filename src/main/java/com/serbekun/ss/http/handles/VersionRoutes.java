package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.v0.ApiVersion;

public class VersionRoutes {

    private final ApiVersion apiVersion;

    public VersionRoutes(ApiVersion apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void register(Javalin svr) {
        svr.get("/api/v0/version", ctx -> apiVersion.main(ctx));
    }
}
