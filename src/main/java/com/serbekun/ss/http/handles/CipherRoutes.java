package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0CipherAesHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.cipher.CipherService;

import io.javalin.Javalin;

/**
 * Routes for AES encryption.
 */
public class CipherRoutes implements HttpHandler {

    private final ApiV0CipherAesHttp apiV0CipherAesHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0CipherAes = new Endpoint("/api/v0/cipher");

    public CipherRoutes(CipherService cipherService, EndpointRegistrar endpointRegistrar) {
        this.apiV0CipherAesHttp = new ApiV0CipherAesHttp(cipherService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers cipher routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0CipherAes, false);

        svr.before("/api/v0/cipher/aes", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/aes/encrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/aes/decrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));

        svr.get("/api/v0/cipher/aes", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/encrypt", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/decrypt", ctx -> apiV0CipherAesHttp.main(ctx));
    }
}
