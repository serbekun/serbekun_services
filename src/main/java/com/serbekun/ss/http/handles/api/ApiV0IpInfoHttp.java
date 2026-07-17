package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.domain.dto.http.network.ApiV0IpInfoResponse;

public class ApiV0IpInfoHttp {

    public void handle(Context ctx) {
        String ip = resolveClientIp(ctx);
        ctx.status(HttpStatus.OK);
        ctx.json(new ApiV0IpInfoResponse(ip));
    }

    // Behind cloudflare -> nginx, ctx.ip() is the proxy address, so the real
    // client ip has to come from forwarding headers. Values are spoofable by
    // clients that reach the server bypassing the proxies — fine for showing
    // the user their own ip, not for auth or rate limiting.
    private String resolveClientIp(Context ctx) {
        String cfIp = ctx.header("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }

        String forwardedFor = ctx.header("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // first entry is the original client, the rest are proxies
            return forwardedFor.split(",")[0].trim();
        }

        return ctx.ip();
    }

}
