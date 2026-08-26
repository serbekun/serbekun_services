package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0QrHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.qr.QrService;

import io.javalin.Javalin;

/**
 * Routes for generating and reading QR codes.
 */
public class QrRoutes implements HttpHandler {

    private final ApiV0QrHttp apiV0QrHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Qr = new Endpoint("/api/v0/qr");

    public QrRoutes(QrService qrService, EndpointRegistrar endpointRegistrar) {
        this.apiV0QrHttp = new ApiV0QrHttp(qrService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers QR routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Qr, false);

        svr.before("/api/v0/qr/generate", ctx -> ctx.attribute("endpoint", endpointApiV0Qr));
        svr.before("/api/v0/qr/read", ctx -> ctx.attribute("endpoint", endpointApiV0Qr));

        svr.post("/api/v0/qr/generate", ctx -> apiV0QrHttp.main(ctx));
        svr.post("/api/v0/qr/read", ctx -> apiV0QrHttp.main(ctx));
    }
}
