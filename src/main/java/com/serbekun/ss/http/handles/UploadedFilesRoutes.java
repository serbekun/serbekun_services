package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.v0.ApiV0UploadedFilesHttp;

/**
 * Routes for working with uploaded files.
 */
public class UploadedFilesRoutes {

    private final ApiV0UploadedFilesHttp apiV0UploadedFilesHttp;

    public UploadedFilesRoutes(ApiV0UploadedFilesHttp apiV0UploadedFilesHttp) {
        this.apiV0UploadedFilesHttp = apiV0UploadedFilesHttp;
    }

    /**
     * Registers uploaded files routes.
     */
    public void register(Javalin svr) {
        svr.get("/api/v0/uploaded-files", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.get("/api/v0/uploaded-files/{uuid}", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.get("/api/v0/uploaded-files/{uuid}/download", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.post("/api/v0/uploaded-files", ctx -> apiV0UploadedFilesHttp.main(ctx));
        svr.delete("/api/v0/uploaded-files/{uuid}", ctx -> apiV0UploadedFilesHttp.main(ctx));
    }
}
