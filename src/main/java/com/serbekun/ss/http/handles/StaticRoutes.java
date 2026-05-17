package com.serbekun.ss.http.handles;

import com.serbekun.ss.http.handles.v0.StaticV0HtmlHttp;
import com.serbekun.ss.http.handles.v0.StaticV0ImagesHttp;
import com.serbekun.ss.http.handles.v0.StaticV0JsonHttp;

import io.javalin.Javalin;

/**
 * Registration of all static routes.
 */
public class StaticRoutes {

    private final IndexHttp index;
    private final StaticV0ImagesHttp staticV0ImagesHttp;
    private final StaticV0JsonHttp staticV0JsonHttp;
    private final StaticV0HtmlHttp staticV0HtmlHttp;

    public StaticRoutes(IndexHttp index, StaticV0ImagesHttp staticV0ImagesHttp,
                        StaticV0JsonHttp staticV0JsonHttp, StaticV0HtmlHttp staticV0HtmlHttp) {
        this.index = index;
        this.staticV0ImagesHttp = staticV0ImagesHttp;
        this.staticV0JsonHttp = staticV0JsonHttp;
        this.staticV0HtmlHttp = staticV0HtmlHttp;
    }

    /**
     * Registers static routes.
     */
    public void register(Javalin svr) {
        svr.get("/", ctx -> index.main(ctx));

        // IMAGES
        svr.get("/static/v0/images/{name}", ctx -> staticV0ImagesHttp.main(ctx));

        // JSON
        svr.get("/static/v0/json", ctx -> staticV0JsonHttp.main(ctx, ""));
        svr.get("/static/v0/json/", ctx -> staticV0JsonHttp.main(ctx, ""));
        svr.get("/static/v0/json/{name}", ctx -> staticV0JsonHttp.main(ctx));

        // HTML
        svr.get("/static/v0/html/{name}", ctx -> staticV0HtmlHttp.main(ctx));
    }
}
