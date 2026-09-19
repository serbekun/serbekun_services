package com.serbekun.ss.http.handles.api;

import java.util.Locale;

import io.javalin.http.Context;

/**
 * Helpers for working out URLs and client addresses from a request.
 */
public final class RequestUrls {

    private RequestUrls() {
    }

    /**
     * Works out the public origin the request should be reached at.
     *
     * @param requested the caller's own {@code baseUrl}, or null to derive one
     * @return the origin without a trailing slash
     * @throws IllegalArgumentException when the given baseUrl is not an http(s) origin
     */
    public static String resolveBaseUrl(Context ctx, String requested) {
        if (requested != null && !requested.isBlank()) {
            String trimmed = requested.strip();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
                throw new IllegalArgumentException("baseUrl must start with http:// or https://");
            }
            return stripTrailingSlash(trimmed);
        }

        // Behind a reverse proxy the request's own scheme and host are the
        // proxy's, not the ones a visitor will have to reach.
        String scheme = firstForwarded(ctx.header("X-Forwarded-Proto"));
        if (scheme == null) {
            scheme = ctx.scheme();
        }
        String host = firstForwarded(ctx.header("X-Forwarded-Host"));
        if (host == null) {
            host = ctx.host();
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("baseUrl is required — the request carries no host");
        }

        return stripTrailingSlash(scheme + "://" + host);
    }

    /** A forwarding header may list every hop; the client-facing one comes first. */
    private static String firstForwarded(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String first = header.split(",")[0].strip();
        return first.isEmpty() ? null : first;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
