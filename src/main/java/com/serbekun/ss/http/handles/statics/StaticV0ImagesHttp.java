package com.serbekun.ss.http.handles.statics;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0ImagesHttp {

    private final ResourcesService resourcesService;

    public StaticV0ImagesHttp(ResourcesService resourcesService) {
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
            ctx.contentType(ContentType.IMAGE_JPEG);
            String files = resourcesService.listImagesAsJson();

            if (files == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            ctx.result(files);
            return;
        }

        ctx.contentType(resourcesService.detectMimeType(name));

        byte[] image = resourcesService.getImage(name);

        if (image == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(image);
    }
}
