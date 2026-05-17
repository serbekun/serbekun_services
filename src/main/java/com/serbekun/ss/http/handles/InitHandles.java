package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.v0.*;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.*;
import com.serbekun.ss.service.http.handles.v0.*;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.Javalin;

public class InitHandles {

    public void initHandles(Javalin svr, ResourcesService resourcesService,
        AuthService authService, EndpointRegistrar endpointRegistrar,

        // API handles services
        StaticV0Json staticV0Json,
        StaticV0Images staticV0Images,
        StaticV0Html staticV0Html,
        ApiV0CatalogsLinks apiV0CatalogsLinks,
        ApiV0CipherAes apiV0CipherAes

    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        StaticV0ImagesHttp staticV0ImagesHttp = new StaticV0ImagesHttp(resourcesService, staticV0Images);
        StaticV0JsonHttp staticV0JsonHttp = new StaticV0JsonHttp(staticV0Json);
        StaticV0HtmlHttp staticV0HtmlHttp = new StaticV0HtmlHttp(staticV0Html);
        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(apiV0CipherAes);
        ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp = new ApiV0CatalogsLinksHttp(apiV0CatalogsLinks);

        EndpointAuthInitializer.initHandlesAuthSetting(svr, endpointRegistrar, authService);

        // routes
        svr.get("/", ctx -> index.main(ctx));

        // == Static ==

        // IMAGES
        svr.get("/static/v0/images/{name}", ctx -> staticV0ImagesHttp.main(ctx));
        
        // JSON
        svr.get("/static/v0/json", ctx -> staticV0JsonHttp.main(ctx, ""));
        svr.get("/static/v0/json/", ctx -> staticV0JsonHttp.main(ctx, ""));
        svr.get("/static/v0/json/{name}", ctx -> staticV0JsonHttp.main(ctx));

        // HTML
        svr.get("/static/v0/html/{name}", ctx -> staticV0HtmlHttp.main(ctx));



        // API

        // AES cipher
        svr.get("/api/v0/cipher/aes", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/encrypt", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/decrypt", ctx -> apiV0CipherAesHttp.main(ctx));

        // Links
        svr.get("/api/v0/catalogs/links", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.post("/api/v0/catalogs/links", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.put("/api/v0/catalogs/links/{uuid}", ctx -> apiV0CatalogsLinksHttp.main(ctx));
        svr.delete("/api/v0/catalogs/links/{uuid}", ctx -> apiV0CatalogsLinksHttp.main(ctx));
    
    }
}
