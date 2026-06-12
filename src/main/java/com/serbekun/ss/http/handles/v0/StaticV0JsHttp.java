package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class StaticV0JsHttp {
    
    private final ResourcesService resourcesService;

    public StaticV0JsHttp(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    } 

    public void main(Context ctx) {
        main(ctx, ctx.pathParam("name"));
    }

    public void main(Context ctx, String name) {
        if (name == null) {
            name = "";
        }

        if (name.isEmpty()) {
            ctx.contentType(ContentType.JSON);
        } else {
            ctx.contentType(ContentType.JAVASCRIPT);
        }

        String js = resourcesService.getJs(name);

        if (js == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(js);
    }
}
