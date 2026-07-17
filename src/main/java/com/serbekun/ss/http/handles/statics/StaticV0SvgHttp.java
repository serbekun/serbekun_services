package com.serbekun.ss.http.handles.statics;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0SvgHttp {

    private final ResourcesService resourcesService;

    public StaticV0SvgHttp(ResourcesService resourcesService) {
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
            ctx.contentType(ContentType.IMAGE_SVG);
        }

        String svg = resourcesService.getSvg(name);

        if (svg == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(svg);
    }
}
