package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.encoding.V0EncodingConvertRequest;
import com.serbekun.ss.domain.dto.http.encoding.V0EncodingRequest;
import com.serbekun.ss.domain.dto.http.encoding.V0EncodingResponse;
import com.serbekun.ss.service.encoding.EncodingFormat;
import com.serbekun.ss.service.encoding.EncodingService;

/**
 * HTTP handler for encoding and data conversion.
 * <p>
 * All seven routes are the same conversion. On {@code /{format}/encode} the
 * target is fixed by the path and {@code encoding} says how to read the input
 * (utf8 by default), so binary can be handed in as hex. On
 * {@code /{format}/decode} the source is fixed and {@code outputEncoding} says
 * how to write the result. {@code /convert} names both sides itself.
 */
public class ApiV0EncodingHttp {

    private static final Logger log = LoggerFactory.getLogger(ApiV0EncodingHttp.class);

    private final EncodingService encodingService;

    public ApiV0EncodingHttp(EncodingService encodingService) {
        this.encodingService = encodingService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        switch (ctx.method()) {
            case POST -> {
                if (ctx.path().endsWith("/convert")) {
                    handleConvert(ctx);
                } else {
                    handleFixed(ctx);
                }
            }
            default -> ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // region fixed endpoints

    /** Handles {@code /base64|hex|url/encode} and {@code /decode}. */
    private void handleFixed(Context ctx) {
        ctx.contentType("application/json");

        V0EncodingRequest request = parseBody(ctx, V0EncodingRequest.class);
        if (request == null || request.data() == null) {
            badRequest(ctx, "'data' is required");
            return;
        }

        String[] segments = ctx.path().split("/");
        boolean encoding = segments[segments.length - 1].equals("encode");
        String pathFormat = segments[segments.length - 2];

        EncodingFormat from;
        EncodingFormat to;
        try {
            // The path names the encoded side; the body names the other one.
            EncodingFormat fixed = pathFormat(pathFormat, request.form());
            from = encoding
                ? EncodingFormat.fromWire(defaultIfBlank(request.encoding(), "utf8"), "encoding")
                : fixed;
            to = encoding
                ? fixed
                : EncodingFormat.fromWire(defaultIfBlank(request.outputEncoding(), "utf8"), "outputEncoding");
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        convert(ctx, request.data(), from, to);
    }

    /**
     * The format the path names.
     *
     * @param form true on a url route to use {@code x-www-form-urlencoded}
     */
    private static EncodingFormat pathFormat(String segment, Boolean form) {
        if (segment.equals("url") && Boolean.TRUE.equals(form)) {
            return EncodingFormat.URL_FORM;
        }
        return EncodingFormat.fromWire(segment, "format");
    }

    // endregion

    // region convert

    private void handleConvert(Context ctx) {
        ctx.contentType("application/json");

        V0EncodingConvertRequest request = parseBody(ctx, V0EncodingConvertRequest.class);
        if (request == null || request.data() == null) {
            badRequest(ctx, "'data' is required");
            return;
        }

        EncodingFormat from;
        EncodingFormat to;
        try {
            from = EncodingFormat.fromWire(request.from(), "from");
            to = EncodingFormat.fromWire(request.to(), "to");
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        convert(ctx, request.data(), from, to);
    }

    // endregion

    /** The one call every route ends in. */
    private void convert(Context ctx, String data, EncodingFormat from, EncodingFormat to) {
        EncodingService.ConvertResult result;
        try {
            result = encodingService.convert(data, from, to);
        } catch (IllegalArgumentException e) {
            // Input that is not valid in its stated format is the caller's to fix.
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Conversion failed", e);
            serverError(ctx, "Conversion failed");
            return;
        }

        ctx.json(new V0EncodingResponse(
            result.data(), result.bytes(), result.from().wireName(), result.to().wireName()));
    }

    // region helpers

    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
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
