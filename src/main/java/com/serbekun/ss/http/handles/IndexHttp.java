package com.serbekun.ss.http.handles;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

public class IndexHttp {

    private static final String HTML_NAME = "index";

    private final ResourcesService resourcesService;

    public IndexHttp(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    public void main(Context ctx) {
        String html = resourcesService.getTextData(ResourcesBasePath.resolveHtmlPath(HTML_NAME));

        if (html == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.html(html);
    }
}
