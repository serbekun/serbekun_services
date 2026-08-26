package com.serbekun.ss.http.handles.api;

import java.util.ArrayList;
import java.util.List;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.id.V0IdBatchItem;
import com.serbekun.ss.domain.dto.http.id.V0IdBatchRequest;
import com.serbekun.ss.domain.dto.http.id.V0IdBatchResponse;
import com.serbekun.ss.domain.dto.http.id.V0IdResponse;
import com.serbekun.ss.service.id.IdService;
import com.serbekun.ss.service.id.IdType;

/**
 * HTTP handler for identifiers and random material.
 * <p>
 * The four {@code GET} routes take their choices as query parameters and each
 * pins one kind of value; {@code /id/batch} takes the same choices as JSON and
 * can mix kinds in one round trip. Values always come back as an array, even
 * when one was asked for, so a client never has to branch on the count.
 */
public class ApiV0IdHttp {

    private static final Logger log = LoggerFactory.getLogger(ApiV0IdHttp.class);

    private final IdService idService;

    public ApiV0IdHttp(IdService idService) {
        this.idService = idService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        ctx.contentType("application/json");

        switch (ctx.method()) {
            case GET -> handleGet(ctx);
            case POST -> handleBatch(ctx);
            default -> ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // region single kind

    private void handleGet(Context ctx) {
        String path = ctx.path();
        IdType type = path.endsWith("/uuid") ? IdType.UUID
            : path.endsWith("/ulid") ? IdType.ULID
            : path.endsWith("/token") ? IdType.TOKEN
            : IdType.BYTES;

        IdService.Spec spec;
        try {
            spec = new IdService.Spec(
                type,
                intParam(ctx, "count"),
                ctx.queryParam("version"),
                ctx.queryParam("format"),
                intParam(ctx, "length"),
                ctx.queryParam("alphabet"),
                ctx.queryParam("chars"));
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        IdService.Result result;
        try {
            result = idService.generate(spec);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Generation failed", e);
            serverError(ctx, "Generation failed");
            return;
        }

        ctx.json(response(result));
    }

    // endregion

    // region batch

    private void handleBatch(Context ctx) {
        List<V0IdBatchItem> items = batchItems(ctx);
        if (items == null || items.isEmpty()) {
            badRequest(ctx, "'items' is required — an array of {type, count, …} objects");
            return;
        }

        List<IdService.Spec> specs = new ArrayList<>(items.size());
        try {
            for (V0IdBatchItem item : items) {
                if (item == null) {
                    throw new IllegalArgumentException("an item cannot be null");
                }
                specs.add(new IdService.Spec(
                    IdType.fromWire(item.type()),
                    item.count(),
                    item.version(),
                    item.format(),
                    item.length(),
                    item.alphabet(),
                    item.chars()));
            }
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        List<IdService.Result> results;
        try {
            results = idService.batch(specs);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Batch generation failed", e);
            serverError(ctx, "Generation failed");
            return;
        }

        List<V0IdResponse> responses = new ArrayList<>(results.size());
        for (IdService.Result result : results) {
            responses.add(response(result));
        }
        ctx.json(new V0IdBatchResponse(responses));
    }

    /**
     * The items of a batch request.
     * <p>
     * A body that is one item on its own is wrapped here, so a single awkward
     * request does not need an {@code items} array around it.
     *
     * @return the items, or null when the body is neither shape
     */
    private List<V0IdBatchItem> batchItems(Context ctx) {
        V0IdBatchRequest batch = parseBody(ctx, V0IdBatchRequest.class);
        if (batch != null && batch.items() != null) {
            return batch.items();
        }

        V0IdBatchItem single = parseBody(ctx, V0IdBatchItem.class);
        return single == null || single.type() == null ? null : List.of(single);
    }

    // endregion

    // region helpers

    private static V0IdResponse response(IdService.Result result) {
        return new V0IdResponse(
            result.type(), result.count(), result.values(), result.format(),
            result.version(), result.alphabet(), result.length(), result.bits());
    }

    /**
     * Reads a whole-number query parameter.
     *
     * @return the value, or null when the parameter was not sent
     * @throws IllegalArgumentException when it was sent but is not a number
     */
    private static Integer intParam(Context ctx, String name) {
        String raw = ctx.queryParam(name);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a whole number");
        }
    }

    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private static void badRequest(Context ctx, String message) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.json(ErrorResponse.of(message));
    }

    private static void serverError(Context ctx, String message) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.json(ErrorResponse.of(message));
    }

    // endregion
}
