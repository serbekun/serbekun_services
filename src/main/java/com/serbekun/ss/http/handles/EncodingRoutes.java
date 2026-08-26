package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.api.ApiV0EncodingHttp;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.encoding.EncodingService;

import io.javalin.Javalin;

/**
 * Routes for encoding and data conversion.
 */
public class EncodingRoutes implements HttpHandler {

    /** The encoded side of each fixed route, as it appears in the path. */
    private static final String[] FIXED_FORMATS = {"base64", "hex", "url"};

    private final ApiV0EncodingHttp apiV0EncodingHttp;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointApiV0Encoding = new Endpoint("/api/v0/encoding");

    public EncodingRoutes(EncodingService encodingService, EndpointRegistrar endpointRegistrar) {
        this.apiV0EncodingHttp = new ApiV0EncodingHttp(encodingService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers encoding routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointApiV0Encoding, false);

        for (String format : FIXED_FORMATS) {
            for (String direction : new String[] {"encode", "decode"}) {
                String path = "/api/v0/encoding/" + format + "/" + direction;
                svr.before(path, ctx -> ctx.attribute("endpoint", endpointApiV0Encoding));
                svr.post(path, ctx -> apiV0EncodingHttp.main(ctx));
            }
        }

        svr.before("/api/v0/encoding/convert", ctx -> ctx.attribute("endpoint", endpointApiV0Encoding));
        svr.post("/api/v0/encoding/convert", ctx -> apiV0EncodingHttp.main(ctx));
    }
}
