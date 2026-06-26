package com.serbekun.ss.http.handles.v0;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0PdfHttp {

    private final ResourcesService resourcesService;

    public StaticV0PdfHttp(ResourcesService resourcesService) {
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
            String files = resourcesService.listPdfsAsJson();

            if (files == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            ctx.result(files);
            return;
        }

        ctx.contentType(resourcesService.detectMimeType(name));

        byte[] pdf = resourcesService.getPdf(name);

        if (pdf == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(pdf);
    }
}
