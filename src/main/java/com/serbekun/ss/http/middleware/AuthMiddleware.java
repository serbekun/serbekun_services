package com.serbekun.ss.http.middleware;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;

import com.serbekun.ss.service.auth.AuthService;
import com.serbekun.ss.service.auth.api.Endpoint;

/**
 * Global auth gate. Reads the "endpoint" attribute set by the per-handler
 * path filters and checks the request token against it.
 * Must be registered AFTER all handlers: Javalin runs before-filters in
 * registration order, and this filter depends on the "endpoint" attribute
 * already being set.
 */
public class AuthMiddleware {

    private final AuthService authService;

    public AuthMiddleware(AuthService authService) {
        this.authService = authService;
    }

    public void register(Javalin server) {
        server.before(ctx -> {
            Endpoint endpoint = ctx.attribute("endpoint");

            if (endpoint == null) {
                return;
            }

            String token = null;

            // Authorization header
            String authHeader = ctx.header("Authorization");
            if (authHeader != null) {
                if (authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7).trim();
                } else {
                    token = authHeader.trim();
                }
            }

            // query ?token=
            if (token == null) {
                token = ctx.queryParam("token");
            }

            // query ?Authorization=Bearer ...
            if (token == null) {
                String authInQuery = ctx.queryParam("Authorization");
                if (authInQuery != null && authInQuery.startsWith("Bearer ")) {
                    token = authInQuery.substring(7).trim();
                }
            }

            boolean authorized = authService.checkAuth(endpoint, token);
            if (!authorized) {
                if (token == null || token.isBlank()) {
                    throw new UnauthorizedResponse("Missing or invalid token");
                }
                throw new UnauthorizedResponse("Unauthorized");
            }

            if (token != null && !token.isBlank()) {
                ctx.attribute("userToken", token);
            }
        });
    }
}
