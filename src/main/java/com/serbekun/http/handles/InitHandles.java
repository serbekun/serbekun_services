package com.serbekun.http.handles;

import io.javalin.Javalin;


public class InitHandles {
    
    public void initHandles(Javalin svr) {

        Index index = new Index();

        svr.get("/", index::Main);
    }
}
