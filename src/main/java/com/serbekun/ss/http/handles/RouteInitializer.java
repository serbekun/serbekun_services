package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.api.ApiV0CatalogsLinksHttp;
import com.serbekun.ss.http.handles.api.ApiV0CipherAesHttp;
import com.serbekun.ss.http.handles.api.ApiV0ShortUrlHttp;
import com.serbekun.ss.http.handles.api.ApiV0UploadedFilesHttp;
import com.serbekun.ss.http.handles.api.ApiV0YoutubeHttp;
import com.serbekun.ss.http.handles.api.ApiVersion;
import com.serbekun.ss.http.handles.statics.StaticV0CssHttp;
import com.serbekun.ss.http.handles.statics.StaticV0DomainHttp;
import com.serbekun.ss.http.handles.statics.StaticV0HtmlHttp;
import com.serbekun.ss.http.handles.statics.StaticV0ImagesHttp;
import com.serbekun.ss.http.handles.statics.StaticV0JsHttp;
import com.serbekun.ss.http.handles.statics.StaticV0JsonHttp;
import com.serbekun.ss.http.handles.statics.StaticV0PdfHttp;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.links.LinksService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.shorturl.ShortUrlService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.YoutubeService;

/**
 * Main Javalin routes initializer.
 * Entry point for registering all HTTP routes and auth settings.
 */
public class RouteInitializer {

    /**
     * Initializes all routes and authorization.
     *
     * @param svr Javalin server
     * @param resourcesService resources service
     * @param authService auth service
     * @param endpointRegistrar endpoint registrar
     * @param staticV0Json JSON service
     * @param staticV0Images images service
     * @param staticV0Html HTML service
     * @param apiV0CatalogsLinks links service
     * @param apiV0CipherAes AES cipher service
     */
    public void init(Javalin svr, ResourcesService resourcesService,
                      AuthService authService, EndpointRegistrar endpointRegistrar,
                      ResourcesService staticResourcesService,
                      LinksService linksService,
                      com.serbekun.ss.service.cipher.CipherService cipherService,
                      YoutubeService youtubeService,
                      UploadedFilesService uploadedFilesService,
                      ShortUrlService shortUrlService) {

        // First initialize auth and endpoints
        AuthInitializer authInitializer = new AuthInitializer();
        authInitializer.init(svr, endpointRegistrar, authService);

        // Create handlers
        IndexHttp index = new IndexHttp(resourcesService);

        // Static resources routes
        StaticV0ImagesHttp staticV0ImagesHttp = new StaticV0ImagesHttp(staticResourcesService);
        StaticV0JsonHttp staticV0JsonHttp = new StaticV0JsonHttp(staticResourcesService);
        StaticV0HtmlHttp staticV0HtmlHttp = new StaticV0HtmlHttp(staticResourcesService);
        StaticV0PdfHttp staticV0PdfHttp = new StaticV0PdfHttp(staticResourcesService);
        StaticV0CssHttp staticV0CssHttp = new StaticV0CssHttp(staticResourcesService);
        StaticV0JsHttp staticV0JsHttp = new StaticV0JsHttp(staticResourcesService);
        StaticV0DomainHttp staticV0DomainHttp = new StaticV0DomainHttp(staticResourcesService);

        // API routes
        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(cipherService);
        ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp = new ApiV0CatalogsLinksHttp(linksService);
        ApiV0YoutubeHttp apiV0YoutubeHttp = new ApiV0YoutubeHttp(youtubeService);
        ApiV0UploadedFilesHttp apiV0UploadedFilesHttp = new ApiV0UploadedFilesHttp(uploadedFilesService);
        ApiV0ShortUrlHttp apiV0ShortUrlHttp = new ApiV0ShortUrlHttp(shortUrlService);
        ApiVersion apiVersion = new ApiVersion();

        // Register routes
        StaticRoutes staticRoutes = new StaticRoutes(index,
            staticV0ImagesHttp,
            staticV0JsonHttp,
            staticV0HtmlHttp,
            staticV0PdfHttp,
            staticV0CssHttp,
            staticV0JsHttp,
            staticV0DomainHttp
            );
            
        staticRoutes.register(svr);

        ApiV0Routes apiV0Routes = new ApiV0Routes(
            new CipherRoutes(apiV0CipherAesHttp),
            new LinkCatalogRoutes(apiV0CatalogsLinksHttp),
            new YoutubeRoutes(apiV0YoutubeHttp),
            new UploadedFilesRoutes(apiV0UploadedFilesHttp),
            new ShortUrlRoutes(apiV0ShortUrlHttp),
            new VersionRoutes(apiVersion)
        );
        apiV0Routes.register(svr);
    }
}
