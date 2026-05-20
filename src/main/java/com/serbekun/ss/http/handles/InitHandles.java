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
