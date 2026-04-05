package com.serbekun.ss.http.handles.v0;


import com.serbekun.ss.service.http.handles.v0.StaticV0Images;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class V0ImagesHttp {

    private final ResourcesService resourcesService;
    private final StaticV0Images v0ResourcesImages;

    public V0ImagesHttp(ResourcesService resourcesService, StaticV0Images v0ResourcesImages) {
        this.resourcesService = resourcesService;
        this.v0ResourcesImages = v0ResourcesImages;
    }

    public void main(Context ctx) {
        String name = ctx.pathParam("name");
        ctx.contentType(resourcesService.detectMimeType(name));

        byte[] image = v0ResourcesImages.run(name);
        
        if (image == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        } 

        ctx.result(image);
    }
}
