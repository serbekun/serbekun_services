package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.statics.StaticV0Http;
import com.serbekun.ss.http.handles.statics.StaticV0Http.StaticResource;

/**
 * Registration of all static routes.
 */
public class StaticRoutes {

    private final IndexHttp index;
    private final StaticV0Http staticV0Http;

    public StaticRoutes(IndexHttp index, StaticV0Http staticV0Http) {
        this.index = index;
        this.staticV0Http = staticV0Http;
    }

    /**
     * Registers static routes.
     */
    public void register(Javalin svr) {
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
