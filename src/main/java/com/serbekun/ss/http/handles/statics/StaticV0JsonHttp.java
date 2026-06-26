package com.serbekun.ss.http.handles.statics;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0JsonHttp {

    private final ResourcesService resourcesService;

    public StaticV0JsonHttp(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    public void main(Context ctx) {
        main(ctx, ctx.pathParam("name"));
    }

    public void main(Context ctx, String name) {
        ctx.contentType("application/json");

        if (name == null) {
            name = "";
        }

        String json = resourcesService.getJson(name);

        if (json == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(json);
    }
}
