package com.serbekun.ss.http.handles;

import com.serbekun.ss.config.Config;
import com.serbekun.ss.http.handles.api.ApiV0UploadedFilesHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;

import io.javalin.Javalin;

/**
 * Routes for working with uploaded files.
 */
public class UploadedFilesRoutes implements HttpHandler {

    private final ApiV0UploadedFilesHttp apiV0UploadedFilesHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0UploadedFiles = new Endpoint("/api/v0/uploaded-files");

    public UploadedFilesRoutes(UploadedFilesService uploadedFilesService, Config config, EndpointRegistrar endpointRegistrar) {
        this.apiV0UploadedFilesHttp = new ApiV0UploadedFilesHttp(uploadedFilesService, config);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers uploaded files routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0UploadedFiles, false);

        svr.before("/api/v0/uploaded-files", ctx -> ctx.attribute("endpoint", endpointApiV0UploadedFiles));
        svr.before("/api/v0/uploaded-files/max-size", ctx -> ctx.attribute("endpoint", endpointApiV0UploadedFiles));
        svr.before("/api/v0/uploaded-files/{uuid}", ctx -> ctx.attribute("endpoint", endpointApiV0UploadedFiles));
        svr.before("/api/v0/uploaded-files/{uuid}/download", ctx -> ctx.attribute("endpoint", endpointApiV0UploadedFiles));

        svr.get("/api/v0/uploaded-files", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.get("/api/v0/uploaded-files/max-size", ctx -> apiV0UploadedFilesHttp.handleMaxSize(ctx));
        svr.get("/api/v0/uploaded-files/{uuid}", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.get("/api/v0/uploaded-files/{uuid}/download", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.post("/api/v0/uploaded-files", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.delete("/api/v0/uploaded-files/{uuid}", ctx -> apiV0UploadedFilesHttp.main(ctx));
    }
}
