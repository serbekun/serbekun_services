package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.service.http.handles.v0.StaticV0Html;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class V0PagesHttp {
    
    private final StaticV0Html v0Page;

    public V0PagesHttp(StaticV0Html v0Page) {
        this.v0Page = v0Page;
    } 

    public void main(Context ctx) {

        ctx.contentType("text/html");

        String name = ctx.pathParam("name");
        String html = v0Page.run(name);

        if (html == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(html);
    }
}
