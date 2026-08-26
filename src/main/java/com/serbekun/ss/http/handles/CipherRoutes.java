package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0CipherAesHttp;
import com.serbekun.ss.http.handles.api.ApiV0CipherHybridHttp;
import com.serbekun.ss.http.handles.api.ApiV0CipherRsaHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.cipher.CipherService;

import io.javalin.Javalin;

/**
 * Routes for AES, RSA and hybrid encryption.
 */
public class CipherRoutes implements HttpHandler {

    private final ApiV0CipherAesHttp apiV0CipherAesHttp;
    private final ApiV0CipherRsaHttp apiV0CipherRsaHttp;
    private final ApiV0CipherHybridHttp apiV0CipherHybridHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0CipherAes = new Endpoint("/api/v0/cipher");

    public CipherRoutes(CipherService cipherService, EndpointRegistrar endpointRegistrar) {
        this.apiV0CipherAesHttp = new ApiV0CipherAesHttp(cipherService);
        this.apiV0CipherRsaHttp = new ApiV0CipherRsaHttp(cipherService);
        this.apiV0CipherHybridHttp = new ApiV0CipherHybridHttp(cipherService);
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

        svr.before("/api/v0/cipher/rsa/keypair", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/rsa/encrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/rsa/decrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/rsa/sign", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/rsa/verify", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));

        svr.before("/api/v0/cipher/hybrid/encrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));
        svr.before("/api/v0/cipher/hybrid/decrypt", ctx -> ctx.attribute("endpoint", endpointApiV0CipherAes));

        svr.get("/api/v0/cipher/aes", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/encrypt", ctx -> apiV0CipherAesHttp.main(ctx));
        svr.post("/api/v0/cipher/aes/decrypt", ctx -> apiV0CipherAesHttp.main(ctx));

        svr.get("/api/v0/cipher/rsa/keypair", ctx -> apiV0CipherRsaHttp.main(ctx));
        svr.post("/api/v0/cipher/rsa/encrypt", ctx -> apiV0CipherRsaHttp.main(ctx));
        svr.post("/api/v0/cipher/rsa/decrypt", ctx -> apiV0CipherRsaHttp.main(ctx));
        svr.post("/api/v0/cipher/rsa/sign", ctx -> apiV0CipherRsaHttp.main(ctx));
        svr.post("/api/v0/cipher/rsa/verify", ctx -> apiV0CipherRsaHttp.main(ctx));

        svr.post("/api/v0/cipher/hybrid/encrypt", ctx -> apiV0CipherHybridHttp.main(ctx));
        svr.post("/api/v0/cipher/hybrid/decrypt", ctx -> apiV0CipherHybridHttp.main(ctx));
    }
}
