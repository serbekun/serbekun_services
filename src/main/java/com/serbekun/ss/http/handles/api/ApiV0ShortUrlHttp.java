package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlDeleteRequest;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlPostRequest;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlPostResponse;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlQrRequest;
import com.serbekun.ss.domain.dto.http.shorturl.V0ShortUrlQrResponse;
import com.serbekun.ss.domain.models.ShortUrl;
import com.serbekun.ss.service.qr.QrService;
import com.serbekun.ss.service.shorturl.ShortUrlService;

/**
 * HTTP handler for shortened URLs — create, resolve (redirect), delete, and
 * create-with-a-QR-code in one call.
 */
public class ApiV0ShortUrlHttp {

    // region fields

    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(ApiV0ShortUrlHttp.class);

    /** Short url service instance */
    private final ShortUrlService service;

    /** Renders the short link as a scannable code */
    private final QrService qrService;

    // endregion

    public ApiV0ShortUrlHttp(ShortUrlService service, QrService qrService) {
        this.service = service;
        this.qrService = qrService;
    }

    // region main handler

    /** Main handler for the API endpoint */
    public void main(Context ctx) {
        switch (ctx.method()) {
            case GET    -> handleGet(ctx);
            case POST   -> {
                if (ctx.path().endsWith("/qr")) {
                    handlePostQr(ctx);
                } else {
                    handlePost(ctx);
                }
            }
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
            ctx.json(ErrorResponse.of("id path parameter is required"));
            return;
        }

        ShortUrl shortUrl = service.getShortUrl(id);
        if (shortUrl == null) {
            ctx.contentType("application/json");
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.json(ErrorResponse.of("Short url not found"));
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
            ctx.json(ErrorResponse.of("'url' is required"));
            return;
        }

        ShortUrl shortUrl;
        try {
            shortUrl = service.createShortUrl(body.url(), body.name(), body.description());
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of(e.getMessage()));
            return;
        }

        ctx.status(HttpStatus.CREATED);
        ctx.json(new V0ShortUrlPostResponse(shortUrl.id(), shortUrl.token()));
    }

    // endregion

    // region qr

    /**
     * Handles POST requests that shorten a url and render the short link as a
     * QR code in one round trip.
     * <p>
     * Takes the same body as plain creation plus the QR options of
     * {@code /api/v0/qr/generate}, and answers with the created record and the
     * code as a {@code data:} URL — the two things a "print this link" page
     * needs, without a second call. The link the code points at is built from
     * the request host unless the caller names a {@code baseUrl}, which is what
     * a deployment behind a custom domain needs.
     */
    private void handlePostQr(Context ctx) {
        ctx.contentType("application/json");

        V0ShortUrlQrRequest body = parseBody(ctx, V0ShortUrlQrRequest.class);
        if (body == null || body.url() == null || body.url().isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of("'url' is required"));
            return;
        }

        QrService.QrOptions options;
        String baseUrl;
        try {
            options = QrService.QrOptions.of(body.format(), body.size(), body.errorCorrection(),
                    body.foreground(), body.background(), body.margin());
            baseUrl = RequestUrls.resolveBaseUrl(ctx, body.baseUrl());
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of(e.getMessage()));
            return;
        }

        // Options are validated above, before anything is stored, so a bad
        // request cannot leave a short url behind. The code itself encodes the
        // short id, so it can only be rendered once the record exists.
        ShortUrl shortUrl;
        QrService.QrImage image;
        try {
            shortUrl = service.createShortUrl(body.url(), body.name(), body.description());
            image = qrService.generate(baseUrl + "/api/v0/short-url/" + shortUrl.id(), options);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of(e.getMessage()));
            return;
        } catch (Exception e) {
            log.error("Failed to render a QR code for a short url", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(ErrorResponse.of("QR generation failed"));
            return;
        }

        ctx.status(HttpStatus.CREATED);
        ctx.json(new V0ShortUrlQrResponse(
                shortUrl.id(),
                shortUrl.token(),
                baseUrl + "/api/v0/short-url/" + shortUrl.id(),
                image.format().wireName(),
                image.contentType(),
                image.size(),
                image.dataUrl()));
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
            ctx.json(ErrorResponse.of("id path parameter is required"));
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
                ctx.json(ErrorResponse.of("Short url not found"));
            }
            case 403 -> {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.json(ErrorResponse.of("Invalid token"));
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
