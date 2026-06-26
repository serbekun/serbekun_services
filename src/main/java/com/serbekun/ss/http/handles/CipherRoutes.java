package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0CipherAesHttp;

import io.javalin.Javalin;

/**
 * Routes for AES encryption.
 */
public class CipherRoutes {

    private final ApiV0CipherAesHttp apiV0CipherAesHttp;

    public CipherRoutes(ApiV0CipherAesHttp apiV0CipherAesHttp) {
        this.apiV0CipherAesHttp = apiV0CipherAesHttp;
    }

    /**
     * Registers cipher routes.
     */
    public void register(Javalin svr) {
        svr.get("/api/v0/cipher/aes", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/encrypt", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/decrypt", ctx -> apiV0CipherAesHttp.main(ctx));
    }
}
