package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;

/**
 * Resolves the client IP behind the cloudflare -> nginx -> server chain.
 * <p>
 * Forwarding headers are spoofable by anyone who can reach the server
 * directly. That is acceptable for display and for the IP allow/deny lists of
 * burn links, which are a convenience filter rather than an authentication
 * boundary.
 */
public final class ClientIps {

    private ClientIps() {
    }

    public static String resolve(Context ctx) {
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
