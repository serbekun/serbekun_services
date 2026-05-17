package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.v0.ApiV0CatalogsLinksHttp;
import com.serbekun.ss.http.handles.v0.ApiV0CipherAesHttp;
import com.serbekun.ss.http.handles.v0.StaticV0HtmlHttp;
import com.serbekun.ss.http.handles.v0.StaticV0ImagesHttp;
import com.serbekun.ss.http.handles.v0.StaticV0JsonHttp;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.http.handles.v0.*;
import com.serbekun.ss.service.resource.ResourcesService;

import io.javalin.Javalin;

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
                     StaticV0Json staticV0Json,
                     StaticV0Images staticV0Images,
                     StaticV0Html staticV0Html,
                     ApiV0CatalogsLinks apiV0CatalogsLinks,
                     ApiV0CipherAes apiV0CipherAes) {

        // First initialize auth and endpoints
        AuthInitializer authInitializer = new AuthInitializer();
        authInitializer.init(svr, endpointRegistrar, authService);

        // Create handlers
        IndexHttp index = new IndexHttp(resourcesService);
        StaticV0ImagesHttp staticV0ImagesHttp = new StaticV0ImagesHttp(resourcesService, staticV0Images);
        StaticV0JsonHttp staticV0JsonHttp = new StaticV0JsonHttp(staticV0Json);
        StaticV0HtmlHttp staticV0HtmlHttp = new StaticV0HtmlHttp(staticV0Html);
        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(apiV0CipherAes);
        ApiV0CatalogsLinksHttp apiV0CatalogsLinksHttp = new ApiV0CatalogsLinksHttp(apiV0CatalogsLinks);

        // Register routes
        StaticRoutes staticRoutes = new StaticRoutes(index, staticV0ImagesHttp, staticV0JsonHttp, staticV0HtmlHttp);
        staticRoutes.register(svr);

        ApiV0Routes apiV0Routes = new ApiV0Routes(
            new CipherRoutes(apiV0CipherAesHttp),
            new LinkCatalogRoutes(apiV0CatalogsLinksHttp)
        );
        apiV0Routes.register(svr);
    }
}
