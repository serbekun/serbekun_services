package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.burnlink.V0BurnLinkDeleteRequest;
import com.serbekun.ss.domain.dto.http.burnlink.V0BurnLinkPostRequest;
import com.serbekun.ss.domain.dto.http.burnlink.V0BurnLinkPostResponse;
import com.serbekun.ss.domain.dto.http.burnlink.V0BurnLinkRevealResponse;
import com.serbekun.ss.domain.models.BurnLink;
import com.serbekun.ss.service.burnlink.BurnLinkService;
import com.serbekun.ss.service.resource.ResourcesService;

/**
 * HTTP handler for self-destructing links.
 * <p>
 * {@code GET /b/{id}} serves the reveal page but never burns. Only
 * {@code POST /api/v0/burn/{id}/reveal} burns, so link-preview fetchers that
 * merely GET the URL cannot destroy the secret. Missing, expired, already
 * burned and access-blocked all answer with the same 404.
 */
public class ApiV0BurnLinkHttp {

    private static final Logger log = LoggerFactory.getLogger(ApiV0BurnLinkHttp.class);

    /** Static page that carries the reveal button. */
    private static final String REVEAL_PAGE = "burn_reveal.html";

    /** Placeholder replaced with the link id in the reveal page. */
    private static final String ID_PLACEHOLDER = "__BURN_LINK_ID__";

    private final BurnLinkService service;
    private final ResourcesService resourcesService;

    public ApiV0BurnLinkHttp(BurnLinkService service, ResourcesService resourcesService) {
        this.service = service;
        this.resourcesService = resourcesService;
    }

    // region create

    /**
     * {@code POST /api/v0/burn} — create a burn link.
     */
    public void handleCreate(Context ctx) {
        ctx.contentType("application/json");

        V0BurnLinkPostRequest body = parseBody(ctx, V0BurnLinkPostRequest.class);
        if (body == null || body.text() == null || body.text().isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of("'text' is required"));
            return;
        }

        long ttl = body.ttl() == null ? 0 : body.ttl();

        String baseUrl;
        try {
            baseUrl = RequestUrls.resolveBaseUrl(ctx, body.baseUrl());
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of(e.getMessage()));
            return;
        }

        BurnLink link;
        try {
            link = service.create(body.text(), body.name(), ttl,
                    body.devices(), body.browsers(), body.ipWhitelist(), body.ipBlacklist());
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of(e.getMessage()));
            return;
        }

        ctx.status(HttpStatus.CREATED);
        ctx.json(new V0BurnLinkPostResponse(
                link.id(),
                link.token(),
                baseUrl + "/b/" + link.id(),
                link.expiredTime(),
                link.devices(),
                link.browsers(),
                link.ipWhitelist(),
                link.ipBlacklist()));
    }

    // endregion

    // region page

    /**
     * {@code GET /b/{id}} — serve the reveal page without burning.
     */
    public void handlePage(Context ctx) {
        String id = pathParam(ctx, "id");
        if (id == null) {
            writeNotFound(ctx);
            return;
        }

        BurnLink link = service.resolve(id, ctx.header("User-Agent"), ClientIps.resolve(ctx));
        if (link == null) {
            writeNotFound(ctx);
            return;
        }

        String html = resourcesService.getHtml(REVEAL_PAGE);
        if (html == null) {
            log.error("Reveal page {} is missing from resources", REVEAL_PAGE);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(ErrorResponse.of("Reveal page unavailable"));
            return;
        }

        ctx.html(html.replace(ID_PLACEHOLDER, escapeHtml(id)));
    }

    // endregion

    // region reveal

    /**
     * {@code POST /api/v0/burn/{id}/reveal} — burn the link and return its text.
     */
    public void handleReveal(Context ctx) {
        ctx.contentType("application/json");

        String id = pathParam(ctx, "id");
        if (id == null) {
            writeNotFound(ctx);
            return;
        }

        String text = service.reveal(id, ctx.header("User-Agent"), ClientIps.resolve(ctx));
        if (text == null) {
            writeNotFound(ctx);
            return;
        }

        ctx.status(HttpStatus.OK);
        ctx.json(new V0BurnLinkRevealResponse(text));
    }

    // endregion

    // region delete

    /**
     * {@code DELETE /api/v0/burn/{id}} — remove the link early. Requires the
     * token issued at creation time.
     */
    public void handleDelete(Context ctx) {
        ctx.contentType("application/json");

        String id = pathParam(ctx, "id");
        if (id == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(ErrorResponse.of("id path parameter is required"));
            return;
        }

        String token = ctx.queryParam("token");
        if (token == null) {
            V0BurnLinkDeleteRequest body = parseBody(ctx, V0BurnLinkDeleteRequest.class);
            if (body != null) {
                token = body.token();
            }
        }

        switch (service.delete(id, token)) {
            case 404 -> {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.json(ErrorResponse.of("Burn link not found"));
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

    private void writeNotFound(Context ctx) {
        ctx.contentType("application/json");
        ctx.status(HttpStatus.NOT_FOUND);
        ctx.json(ErrorResponse.of("Burn link not found"));
    }

    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private String pathParam(Context ctx, String name) {
        return ctx.pathParamMap().get(name);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // endregion
}
