package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.v0.*;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.*;

import com.serbekun.ss.service.links.LinksService;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.Javalin;

public class InitHandles {

    public void initHandles(Javalin svr, ResourcesService resourcesService,
        AuthService authService, EndpointRegistrar endpointRegistrar,
        ResourcesService staticResourcesService,
        LinksService linksService,
        com.serbekun.ss.service.cipher.CipherService cipherService

    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        StaticV0ImagesHttp staticV0ImagesHttp = new StaticV0ImagesHttp(staticResourcesService);
        StaticV0JsonHttp staticV0JsonHttp = new StaticV0JsonHttp(staticResourcesService);
        StaticV0HtmlHttp staticV0HtmlHttp = new StaticV0HtmlHttp(staticResourcesService);
        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(cipherService);
        ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp = new ApiV0CatalogsLinksHttp(linksService);

        EndpointAuthInitializer.initHandlesAuthSetting(svr, endpointRegistrar, authService);

        // routes
        StaticRoutes staticRoutes = new StaticRoutes(index, staticV0ImagesHttp, staticV0JsonHttp, staticV0HtmlHttp);
        staticRoutes.register(svr);

        // API
        ApiV0Routes apiV0Routes = new ApiV0Routes(
            new CipherRoutes(apiV0CipherAesHttp),
            new LinkCatalogRoutes(apiV0CatalogsLinksHttp)
        );
        apiV0Routes.register(svr);
    }
}
