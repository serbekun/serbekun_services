package com.serbekun.ss.http;

import java.util.List;

import io.javalin.Javalin;

import com.serbekun.ss.http.handles.HttpHandler;
import com.serbekun.ss.http.middleware.AuthMiddleware;
import com.serbekun.ss.http.middleware.CharsetMiddleware;

/**
 * Registers server-level middleware and all self-registering handlers.
 */
public class InitHttp {

    private final CharsetMiddleware charsetMiddleware;
    private final List<HttpHandler> handlers;
    private final AuthMiddleware authMiddleware;

    public InitHttp(CharsetMiddleware charsetMiddleware, List<HttpHandler> handlers, AuthMiddleware authMiddleware) {
        this.charsetMiddleware = charsetMiddleware;
        this.handlers = handlers;
        this.authMiddleware = authMiddleware;
    }

    /**
     * Order is contractual: charset filter first, then handlers (routes plus
     * their path→endpoint before-filters), then the auth gate STRICTLY last —
     * the gate reads the "endpoint" attribute set by the handlers' filters,
     * and Javalin runs before-filters in registration order.
     */
    public void register(Javalin server) {
        charsetMiddleware.register(server);

        for (HttpHandler handler : handlers) {
            handler.register(server);
        }

        authMiddleware.register(server);
    }
}
