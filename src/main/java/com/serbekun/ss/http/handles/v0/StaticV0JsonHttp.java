package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.service.http.handles.v0.StaticV0Json;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class StaticV0JsonHttp {

    private final StaticV0Json staticV0Json;

    public StaticV0JsonHttp(StaticV0Json staticV0Json) {
        this.staticV0Json = staticV0Json;
    }

    public void main(Context ctx) {
        ctx.contentType("application/json");
        
        String name = ctx.pathParam("name");
        String json = staticV0Json.run(name);

        if (json == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(json);
    }
}