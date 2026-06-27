package com.serbekun.ss.http.handles.statics;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0DomainHttp {

    private final ResourcesService resourcesService;

    public StaticV0DomainHttp(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    public void main(Context ctx) {
        main(ctx, ctx.pathParam("name"));
    }

    public void main(Context ctx, String name) {
        ctx.contentType("text/plain; charset=utf-8");

        if (name == null) {
            name = "";
        }

        if (name.isEmpty()) {
            String json = resourcesService.listDomainsAsJson();
            if (json == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }
            ctx.contentType("application/json");
            ctx.result(json);
            return;
        }

        String domainData = resourcesService.getDomain(name);

        if (domainData == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(domainData);
    }
}
