package com.serbekun.ss.http.handles;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.statics.*;

/**
 * Registration of all static routes.
 */
public class StaticRoutes {

    private final IndexHttp index;
    private final StaticV0ImagesHttp staticV0ImagesHttp;
    private final StaticV0JsonHttp staticV0JsonHttp;
    private final StaticV0HtmlHttp staticV0HtmlHttp;
    private final StaticV0PdfHttp staticV0PdfHttp;
    private final StaticV0CssHttp staticV0CssHttp;
    private final StaticV0JsHttp staticV0JsHttp;
    private final StaticV0DomainHttp staticV0DomainHttp;

    public StaticRoutes(IndexHttp index, StaticV0ImagesHttp staticV0ImagesHttp,
                        StaticV0JsonHttp staticV0JsonHttp, StaticV0HtmlHttp staticV0HtmlHttp,
                        StaticV0PdfHttp staticV0PdfHttp, StaticV0CssHttp staticV0CssHttp,
                        StaticV0JsHttp staticV0JsHttp,
                        StaticV0DomainHttp staticV0DomainHttp) {
        this.index = index;
        this.staticV0ImagesHttp = staticV0ImagesHttp;
        this.staticV0JsonHttp = staticV0JsonHttp;
        this.staticV0HtmlHttp = staticV0HtmlHttp;
        this.staticV0PdfHttp = staticV0PdfHttp;
        this.staticV0CssHttp = staticV0CssHttp;
        this.staticV0JsHttp = staticV0JsHttp;
        this.staticV0DomainHttp = staticV0DomainHttp;
    }

    /**
     * Registers static routes.
     */
    public void register(Javalin svr) {
        svr.get("/", ctx -> index.main(ctx));

        // IMAGES
        svr.get("/static/v0/images", ctx -> staticV0ImagesHttp.main(ctx, ""));
        svr.get("/static/v0/images/", ctx -> staticV0ImagesHttp.main(ctx, ""));
        svr.get("/static/v0/images/{name}", ctx -> staticV0ImagesHttp.main(ctx));

        // JSON
        svr.get("/static/v0/json", ctx -> staticV0JsonHttp.main(ctx, ""));
        svr.get("/static/v0/json/", ctx -> staticV0JsonHttp.main(ctx, ""));
        svr.get("/static/v0/json/{name}", ctx -> staticV0JsonHttp.main(ctx));

        // HTML
        svr.get("/static/v0/html", ctx -> staticV0HtmlHttp.main(ctx, ""));
        svr.get("/static/v0/html/", ctx -> staticV0HtmlHttp.main(ctx, ""));
        svr.get("/static/v0/html/{name}", ctx -> staticV0HtmlHttp.main(ctx));

        // CSS
        svr.get("/static/v0/css", ctx -> staticV0CssHttp.main(ctx, ""));
        svr.get("/static/v0/css/", ctx -> staticV0CssHttp.main(ctx, ""));
        svr.get("/static/v0/css/{name}", ctx -> staticV0CssHttp.main(ctx));

        // JS
        svr.get("/static/v0/js", ctx -> staticV0JsHttp.main(ctx, ""));
        svr.get("/static/v0/js/", ctx -> staticV0JsHttp.main(ctx, ""));
        svr.get("/static/v0/js/{name}", ctx -> staticV0JsHttp.main(ctx));

        // PDF
        svr.get("/static/v0/pdf", ctx -> staticV0PdfHttp.main(ctx, ""));
        svr.get("/static/v0/pdf/", ctx -> staticV0PdfHttp.main(ctx, ""));
        svr.get("/static/v0/pdf/{name}", ctx -> staticV0PdfHttp.main(ctx));

        // DOMAIN
        svr.get("/static/v0/domain", ctx -> staticV0DomainHttp.main(ctx, ""));
        svr.get("/static/v0/domain/", ctx -> staticV0DomainHttp.main(ctx, ""));
        svr.get("/static/v0/domain/{name}", ctx -> staticV0DomainHttp.main(ctx));
    }
}
