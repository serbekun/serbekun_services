package com.serbekun.http.handles;

import com.serbekun.resources.Resources;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class Index {
    
    public void Main(Context ctx) {
        ctx.status(HttpStatus.OK);
        ctx.html(Resources.getHtml("index"));
    }
}
