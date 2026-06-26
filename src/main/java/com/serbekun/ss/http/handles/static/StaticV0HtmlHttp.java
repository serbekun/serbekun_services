package com.serbekun.ss.http.handles.v0;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0HtmlHttp {
    
    private final ResourcesService resourcesService;

    public StaticV0HtmlHttp(ResourcesService resourcesService) {
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
            ctx.contentType("application/json");
        } else {
            ctx.contentType("text/html");
        }

        String html = resourcesService.getHtml(name);

        if (html == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(html);
    }
}
