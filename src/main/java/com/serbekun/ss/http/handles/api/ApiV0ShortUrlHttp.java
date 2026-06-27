package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlDeleteRequest;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlPostRequest;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlPostResponse;
import com.serbekun.ss.domain.models.ShortUrl;
import com.serbekun.ss.service.shorturl.ShortUrlService;

/**
 * HTTP handler for shortened URLs — create, resolve (redirect), delete.
 */
public class ApiV0ShortUrlHttp {

    // region fields

    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(ApiV0ShortUrlHttp.class);

    /** Object mapper instance */
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Short url service instance */
    private final ShortUrlService service;

    // endregion

    public ApiV0ShortUrlHttp(ShortUrlService service) {
        this.service = service;
    }

    // region main handler

    /** Main handler for the API endpoint */
    public void main(Context ctx) {
        switch (ctx.method()) {
            case GET    -> handleGet(ctx);
            case POST   -> handlePost(ctx);
            case DELETE -> handleDelete(ctx);
            default     -> ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // region get

    /**
     * Handles GET requests for a short url.
     * <p>
     * If a record for the given id exists, responds with a redirect to the
     * target URL. Otherwise responds with 404 Not Found.
     */
    private void handleGet(Context ctx) {
        String id = pathParam(ctx, "id");
        if (id == null) {
            ctx.contentType("application/json");
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"id path parameter is required\"}");
            return;
        }

        ShortUrl shortUrl = service.getShortUrl(id);
        if (shortUrl == null) {
            ctx.contentType("application/json");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.result("{\"error\":\"Short url not found\"}");
            return;
        }

        ctx.redirect(shortUrl.targetUrl());
    }

    // endregion

    // region post

    /**
     * Handles POST requests for creating a short url.
     * <p>
     * Expects a JSON body with a required {@code url} and optional {@code name}
     * and {@code description}. On success responds with 201 Created and the
     * generated short id together with the delete token.
     */
    private void handlePost(Context ctx) {
        ctx.contentType("application/json");

        V0ShortUrlPostRequest body = parseBody(ctx, V0ShortUrlPostRequest.class);
        if (body == null || body.url() == null || body.url().isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"'url' is required\"}");
            return;
        }

        ShortUrl shortUrl;
        try {
            shortUrl = service.createShortUrl(body.url(), body.name(), body.description());
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"" + e.getMessage() + "\"}");
            return;
        }

        ctx.status(HttpStatus.CREATED);
        try {
            ctx.result(mapper.writeValueAsString(
                    new V0ShortUrlPostResponse(shortUrl.id(), shortUrl.token())));
        } catch (Exception e) {
            log.error("Failed to serialize short url response", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // endregion

    // region delete

    /**
     * Handles DELETE requests for a short url.
     * <p>
     * Requires the short id as a path parameter and the delete token issued at
     * creation time, supplied either as a {@code ?token=} query param or in the
     * JSON body. Responds with 204 on success, 403 on token mismatch, 404 if
     * not found.
     */
    private void handleDelete(Context ctx) {
        ctx.contentType("application/json");

        String id = pathParam(ctx, "id");
        if (id == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"id path parameter is required\"}");
            return;
        }

        // Token from query param or request body
        String token = ctx.queryParam("token");
        if (token == null) {
            V0ShortUrlDeleteRequest body = parseBody(ctx, V0ShortUrlDeleteRequest.class);
            if (body != null) {
                token = body.token();
            }
        }

        int status = service.deleteShortUrl(id, token);
        switch (status) {
            case 404 -> {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.result("{\"error\":\"Short url not found\"}");
            }
            case 403 -> {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.result("{\"error\":\"Invalid token\"}");
            }
            default -> ctx.status(HttpStatus.NO_CONTENT);
        }
    }

    // endregion

    // region helpers

    /**
     * Parses the request body into an object of the specified class.
     * @param ctx the HTTP context
     * @param clazz the class to parse into
     * @return the parsed object or null if parsing fails
     */
    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves a path parameter from the HTTP context.
     * @param ctx the HTTP context
     * @param name the parameter name
     * @return the parameter value or null if not found
     */
    private String pathParam(Context ctx, String name) {
        if (ctx.pathParamMap().containsKey(name)) {
            return ctx.pathParamMap().get(name);
        }
        return null;
    }

    // endregion
    // endregion
}
