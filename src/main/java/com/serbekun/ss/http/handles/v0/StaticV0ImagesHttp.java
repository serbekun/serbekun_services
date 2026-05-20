package com.serbekun.ss.http.handles.v0;


import com.serbekun.ss.service.http.handles.v0.StaticV0Images;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class StaticV0ImagesHttp {

    private final ResourcesService resourcesService;
    private final StaticV0Images staticV0Images;

    public StaticV0ImagesHttp(ResourcesService resourcesService, StaticV0Images staticV0Images) {
        this.resourcesService = resourcesService;
        this.staticV0Images = staticV0Images;
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
            String files = staticV0Images.list();

            if (files == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            ctx.result(files);
            return;
        }

        ctx.contentType(resourcesService.detectMimeType(name));

        byte[] image = staticV0Images.run(name);

        if (image == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(image);
    }
}
