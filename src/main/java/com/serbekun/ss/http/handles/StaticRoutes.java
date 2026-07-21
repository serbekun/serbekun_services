package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.statics.StaticV0Http;
import com.serbekun.ss.http.handles.statics.StaticV0Http.StaticResource;
import com.serbekun.ss.service.auth.api.Endpoint;
import com.serbekun.ss.service.auth.api.EndpointRegistrar;
import com.serbekun.ss.service.resource.ResourcesService;

/**
 * Registration of all static routes.
 */
public class StaticRoutes implements HttpHandler {

    private final IndexHttp index;
    private final StaticV0Http staticV0Http;
    private final EndpointRegistrar endpointRegistrar;

    private final Endpoint endpointIndex = new Endpoint("/index");
    private final Endpoint endpointStaticV0Images = new Endpoint("/static/v0/images");
    private final Endpoint endpointStaticV0Json = new Endpoint("/static/v0/json");
    private final Endpoint endpointStaticV0Html = new Endpoint("/static/v0/html/");

    public StaticRoutes(ResourcesService resourcesService, EndpointRegistrar endpointRegistrar) {
        this.index = new IndexHttp(resourcesService);
        this.staticV0Http = new StaticV0Http(resourcesService);
        this.endpointRegistrar = endpointRegistrar;
    }

    /**
     * Registers static routes.
     */
    @Override
    public void register(Javalin svr) {
        endpointRegistrar.register(endpointIndex, false);
        endpointRegistrar.register(endpointStaticV0Images, false);
        endpointRegistrar.register(endpointStaticV0Json, false);
        endpointRegistrar.register(endpointStaticV0Html, false);

        svr.before("/", ctx -> ctx.attribute("endpoint", endpointIndex));
        svr.before("/static/v0/images/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Images));
        svr.before("/static/v0/json", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/json/", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/json/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Json));
        svr.before("/static/v0/html/{name}", ctx -> ctx.attribute("endpoint", endpointStaticV0Html));

        svr.get("/", ctx -> index.main(ctx));
        svr.get("/icon", ctx -> staticV0Http.main(ctx, "ss_icon.svg", StaticResource.SVG));

        // IMAGES
        svr.get("/static/v0/images", ctx -> staticV0Http.main(ctx, "", StaticResource.IMAGES));
        svr.get("/static/v0/images/", ctx -> staticV0Http.main(ctx, "", StaticResource.IMAGES));
        svr.get("/static/v0/images/{name}", ctx -> staticV0Http.main(ctx, StaticResource.IMAGES));

        // JSON
        svr.get("/static/v0/json", ctx -> staticV0Http.main(ctx, "", StaticResource.JSON));
        svr.get("/static/v0/json/", ctx -> staticV0Http.main(ctx, "", StaticResource.JSON));
        svr.get("/static/v0/json/{name}", ctx -> staticV0Http.main(ctx, StaticResource.JSON));

        // HTML
        svr.get("/static/v0/html", ctx -> staticV0Http.main(ctx, "", StaticResource.HTML));
        svr.get("/static/v0/html/", ctx -> staticV0Http.main(ctx, "", StaticResource.HTML));
        svr.get("/static/v0/html/{name}", ctx -> staticV0Http.main(ctx, StaticResource.HTML));

        // CSS
        svr.get("/static/v0/css", ctx -> staticV0Http.main(ctx, "", StaticResource.CSS));
        svr.get("/static/v0/css/", ctx -> staticV0Http.main(ctx, "", StaticResource.CSS));
        svr.get("/static/v0/css/{name}", ctx -> staticV0Http.main(ctx, StaticResource.CSS));

        // JS
        svr.get("/static/v0/js", ctx -> staticV0Http.main(ctx, "", StaticResource.JS));
        svr.get("/static/v0/js/", ctx -> staticV0Http.main(ctx, "", StaticResource.JS));
        svr.get("/static/v0/js/{name}", ctx -> staticV0Http.main(ctx, StaticResource.JS));

        // SVG
        svr.get("/static/v0/svg", ctx -> staticV0Http.main(ctx, "", StaticResource.SVG));
        svr.get("/static/v0/svg/", ctx -> staticV0Http.main(ctx, "", StaticResource.SVG));
        svr.get("/static/v0/svg/{name}", ctx -> staticV0Http.main(ctx, StaticResource.SVG));

        // PDF
        svr.get("/static/v0/pdf", ctx -> staticV0Http.main(ctx, "", StaticResource.PDF));
        svr.get("/static/v0/pdf/", ctx -> staticV0Http.main(ctx, "", StaticResource.PDF));
        svr.get("/static/v0/pdf/{name}", ctx -> staticV0Http.main(ctx, StaticResource.PDF));

        // DOMAIN
        svr.get("/static/v0/domain", ctx -> staticV0Http.main(ctx, "", StaticResource.DOMAIN));
        svr.get("/static/v0/domain/", ctx -> staticV0Http.main(ctx, "", StaticResource.DOMAIN));
        svr.get("/static/v0/domain/{name}", ctx -> staticV0Http.main(ctx, StaticResource.DOMAIN));
    }
}
