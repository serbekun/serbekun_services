package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.service.http.handles.v0.StaticV0Html;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class StaticV0HtmlHttp {
    
    private final StaticV0Html staticV0Html;

    public StaticV0HtmlHttp(StaticV0Html staticV0Html) {
        this.staticV0Html = staticV0Html;
    } 

    public void main(Context ctx) {
        main(ctx, ctx.pathParam("name"));
    }

    public void main(Context ctx, String name) {
        if (name == null) {
            name = "";
        }

        if (name.isEmpty()) {
            ctx.contentType("application/json");
        } else {
            ctx.contentType("text/html");
        }

        String html = staticV0Html.run(name);

        if (html == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(html);
    }
}
