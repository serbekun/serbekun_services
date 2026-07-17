package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.domain.dto.http.network.ApiV0IpInfoResponse;

public class ApiV0IpInfoHttp {
    
    public void handle(Context ctx) {
        String ip = ctx.ip();
        ctx.status(HttpStatus.OK);
        ctx.json(new ApiV0IpInfoResponse(ip));
    }

}
