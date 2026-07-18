package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.config.Config;
import com.serbekun.ss.http.handles.api.ApiV0CipherAesHttp;
import com.serbekun.ss.http.handles.api.ApiV0IpInfoHttp;
import com.serbekun.ss.http.handles.api.ApiV0RepositoryLinksHttp;
import com.serbekun.ss.http.handles.api.ApiV0ShortUrlHttp;
import com.serbekun.ss.http.handles.api.ApiV0UploadedFilesHttp;
import com.serbekun.ss.http.handles.api.ApiV0YoutubeHttp;
import com.serbekun.ss.http.handles.api.ApiVersion;
import com.serbekun.ss.http.handles.statics.StaticV0Http;
import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;
import com.serbekun.ss.service.resource.ResourcesService;
import com.serbekun.ss.service.shorturl.ShortUrlService;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;
import com.serbekun.ss.service.youtube.YoutubeService;

public class RouteInitializer {

    public void initHandles(Javalin svr, ResourcesService resourcesService,
        AuthService authService, EndpointRegistrar endpointRegistrar,
        ResourcesService staticResourcesService,
        LinkRepositoryService linkRepositoryService,
        CipherService cipherService,
        YoutubeService youtubeService,
        UploadedFilesService uploadedFilesService,
        ShortUrlService shortUrlService,
        Config config
    ) {

        // init handles objects
        IndexHttp index = new IndexHttp(resourcesService);
        StaticV0Http staticV0Http = new StaticV0Http(staticResourcesService);


        ApiV0CipherAesHttp apiV0CipherAesHttp = new ApiV0CipherAesHttp(cipherService);
        ApiV0RepositoryLinksHttp apiV0RepositoryLinksHttp = new ApiV0RepositoryLinksHttp(linkRepositoryService);
        ApiV0YoutubeHttp apiV0YoutubeHttp = new ApiV0YoutubeHttp(youtubeService);
        ApiV0UploadedFilesHttp apiV0UploadedFilesHttp = new ApiV0UploadedFilesHttp(uploadedFilesService, config);
        ApiV0ShortUrlHttp apiV0ShortUrlHttp = new ApiV0ShortUrlHttp(shortUrlService);
        ApiV0IpInfoHttp apiV0IpInfoHttp = new ApiV0IpInfoHttp();
        ApiVersion apiVersion = new ApiVersion();

        AuthInitializer.initHandlesAuthSetting(svr, endpointRegistrar, authService);

        // routes
        StaticRoutes staticRoutes = new StaticRoutes(index, staticV0Http);
        staticRoutes.register(svr);

        // API
        ApiV0Routes apiV0Routes = new ApiV0Routes(
            new CipherRoutes(apiV0CipherAesHttp),
            new RepositoryLinksRoutes(apiV0RepositoryLinksHttp),
            new YoutubeRoutes(apiV0YoutubeHttp),
            new UploadedFilesRoutes(apiV0UploadedFilesHttp),
            new ShortUrlRoutes(apiV0ShortUrlHttp),
            new VersionRoutes(apiVersion),
            new NetworkRoutes(apiV0IpInfoHttp)
        );
        apiV0Routes.register(svr);
    }
}
