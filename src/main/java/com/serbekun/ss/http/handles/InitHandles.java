package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.v0.*;
import com.serbekun.ss.service.auth.api.*;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.links.LinksService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.YoutubeService;

public class InitHandles {

    public void initHandles(Javalin svr, ResourcesService resourcesService,
        AuthService authService, EndpointRegistrar endpointRegistrar,
        ResourcesService staticResourcesService,
        LinksService linksService,
        CipherService cipherService,
        YoutubeService youtubeService,
        UploadedFilesService uploadedFilesService
    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        StaticV0ImagesHttp staticV0ImagesHttp = new StaticV0ImagesHttp(staticResourcesService);
        StaticV0JsonHttp staticV0JsonHttp = new StaticV0JsonHttp(staticResourcesService);
        StaticV0HtmlHttp staticV0HtmlHttp = new StaticV0HtmlHttp(staticResourcesService);
        StaticV0PdfHttp staticV0PdfHttp = new StaticV0PdfHttp(staticResourcesService);
        StaticV0CssHttp staticV0CssHttp = new StaticV0CssHttp(staticResourcesService);
        StaticV0JsHttp staticV0JsHttp = new StaticV0JsHttp(staticResourcesService);


        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(cipherService);
        ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp = new ApiV0CatalogsLinksHttp(linksService);
        ApiV0YoutubeHttp apiV0YoutubeHttp = new ApiV0YoutubeHttp(youtubeService);
        ApiV0UploadedFilesHttp apiV0UploadedFilesHttp = new ApiV0UploadedFilesHttp(uploadedFilesService);

        EndpointAuthInitializer.initHandlesAuthSetting(svr, endpointRegistrar, authService);

        // routes
        StaticRoutes staticRoutes = new StaticRoutes(index, staticV0ImagesHttp, staticV0JsonHttp, staticV0HtmlHttp, staticV0PdfHttp, staticV0CssHttp, staticV0JsHttp);
        staticRoutes.register(svr);

        // API
        ApiV0Routes apiV0Routes = new ApiV0Routes(
            new CipherRoutes(apiV0CipherAesHttp),
            new LinkCatalogRoutes(apiV0CatalogsLinksHttp),
            new YoutubeRoutes(apiV0YoutubeHttp),
            new UploadedFilesRoutes(apiV0UploadedFilesHttp)
        );
        apiV0Routes.register(svr);
    }
}
