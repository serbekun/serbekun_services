package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.json.V0JsonDiffRequest;
import com.serbekun.ss.domain.dto.http.json.V0JsonDiffResponse;
import com.serbekun.ss.domain.dto.http.json.V0JsonQueryResponse;
import com.serbekun.ss.domain.dto.http.json.V0JsonValidateResponse;
import com.serbekun.ss.service.json.JsonService;

/**
 * HTTP handler for working with JSON documents.
 * <p>
 * <b>The request body is the document itself</b>, not a wrapper around it, and
 * the options ride in the query string. That is what makes these usable from a
 * shell — {@code curl --data-binary @file.json} — and it is the only shape that
 * can accept a broken document at all, since invalid JSON cannot be quoted
 * inside a JSON envelope. {@code /diff} is the exception: it needs two
 * documents, so it takes {@code {"from": …, "to": …}}.
 * <p>
 * {@code /format} and {@code /minify} answer with the document itself for the
 * same reason — the output of a formatter is a JSON document, and a caller
 * should not have to unescape it out of a field.
 */
public class ApiV0JsonHttp {

    private static final Logger log = LoggerFactory.getLogger(ApiV0JsonHttp.class);

    private final JsonService jsonService;

    public ApiV0JsonHttp(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        if (ctx.method() != io.javalin.http.HandlerType.POST) {
            ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String path = ctx.path();
        if (path.endsWith("/validate")) {
            handleValidate(ctx);
        } else if (path.endsWith("/format")) {
            handleWrite(ctx, true);
        } else if (path.endsWith("/minify")) {
            handleWrite(ctx, false);
        } else if (path.endsWith("/query")) {
            handleQuery(ctx);
        } else {
            handleDiff(ctx);
        }
    }

    // region validate

    private void handleValidate(Context ctx) {
        ctx.contentType("application/json");

        JsonService.Validation validation = jsonService.validate(ctx.body());

        // Invalid is an answer, not a failure: saying which line the comma is
        // missing from is the whole job of this endpoint.
        ctx.json(new V0JsonValidateResponse(
            validation.valid(), validation.error(), validation.line(), validation.column(),
            validation.bytes()));
    }

    // endregion

    // region format and minify

    private void handleWrite(Context ctx, boolean pretty) {
        boolean sortKeys = booleanParam(ctx, "sort");

        String written;
        try {
            written = pretty
                ? jsonService.format(ctx.body(), ctx.queryParam("indent"), sortKeys)
                : jsonService.minify(ctx.body(), sortKeys);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Failed to write the document", e);
            serverError(ctx, "Failed to write the document");
            return;
        }

        // The answer is a JSON document, so it is sent as one rather than
        // escaped into a field of another.
        ctx.contentType("application/json");
        ctx.result(written);
    }

    // endregion

    // region query

    private void handleQuery(Context ctx) {
        ctx.contentType("application/json");

        // `pointer` and `path` say which language they are in by their own
        // name; `expression` leaves it to be worked out, or to `syntax`.
        String syntax = ctx.queryParam("syntax");
        String expression = ctx.queryParam("expression");
        if (expression == null && ctx.queryParam("pointer") != null) {
            expression = ctx.queryParam("pointer");
            syntax = syntax == null ? "pointer" : syntax;
        }
        if (expression == null && ctx.queryParam("path") != null) {
            expression = ctx.queryParam("path");
            syntax = syntax == null ? "jsonpath" : syntax;
        }
        if (expression == null) {
            badRequest(ctx, "an expression is required — pass 'pointer', 'path' or 'expression'");
            return;
        }

        JsonService.QueryResult result;
        try {
            result = jsonService.query(ctx.body(), expression, syntax);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Query failed", e);
            serverError(ctx, "Query failed");
            return;
        }

        ctx.json(new V0JsonQueryResponse(
            result.expression(), result.syntax().wireName(), result.matches().size(),
            result.matches(), result.paths()));
    }

    // endregion

    // region diff

    private void handleDiff(Context ctx) {
        ctx.contentType("application/json");

        V0JsonDiffRequest request;
        try {
            request = ctx.bodyAsClass(V0JsonDiffRequest.class);
        } catch (Exception e) {
            badRequest(ctx, "the body must be JSON of the shape {\"from\": …, \"to\": …}");
            return;
        }

        if (request == null || request.from() == null || request.to() == null) {
            badRequest(ctx, "both 'from' and 'to' are required");
            return;
        }

        JsonService.DiffResult result;
        try {
            result = jsonService.diff(request.from(), request.to());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Diff failed", e);
            serverError(ctx, "Diff failed");
            return;
        }

        ctx.json(new V0JsonDiffResponse(result.equal(), result.operations(), result.patch()));
    }

    // endregion

    // region helpers

    /** A flag is on when it is present and not explicitly false — {@code ?sort} alone counts. */
    private static boolean booleanParam(Context ctx, String name) {
        String raw = ctx.queryParam(name);
        if (raw == null) {
            return false;
        }
        String value = raw.strip().toLowerCase(java.util.Locale.ROOT);
        return value.isEmpty() || value.equals("true") || value.equals("1") || value.equals("yes");
    }

    private static void badRequest(Context ctx, String message) {
        ctx.contentType("application/json");
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.json(ErrorResponse.of(message));
    }

    private static void serverError(Context ctx, String message) {
        ctx.contentType("application/json");
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.json(ErrorResponse.of(message));
    }

    // endregion
}
