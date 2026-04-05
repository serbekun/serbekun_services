package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.service.http.handles.v0.V0ApiJson;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class V0JsonHttp {

    private final V0ApiJson v0ApiJson;

    public V0JsonHttp(V0ApiJson v0ApiJson) {
        this.v0ApiJson = v0ApiJson;
    }

    public void main(Context ctx) {
        ctx.contentType("application/json");
        
        String name = ctx.pathParam("name");
        String json = v0ApiJson.run(name);

        if (json == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(json);
    }
}
