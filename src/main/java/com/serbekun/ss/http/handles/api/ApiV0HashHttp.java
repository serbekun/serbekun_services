package com.serbekun.ss.http.handles.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.hash.*;
import com.serbekun.ss.service.hash.HashAlgorithm;
import com.serbekun.ss.service.hash.HashService;

/**
 * HTTP handler for hashing and integrity checking.
 * <p>
 * {@code data} is UTF-8 text by default; an {@code encoding} of {@code base64}
 * or {@code hex} switches how both {@code data} and {@code key} are read, which
 * is what makes binary payloads and binary HMAC secrets expressible in JSON.
 * Digests come back as lowercase hex.
 */
public class ApiV0HashHttp {

    private static final Logger log = LoggerFactory.getLogger(ApiV0HashHttp.class);

    /** Multipart field the file endpoint reads. */
    private static final String FILE_FIELD = "file";

    private final HashService hashService;

    public ApiV0HashHttp(HashService hashService) {
        this.hashService = hashService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        switch (ctx.method()) {
            case POST -> {
                String path = ctx.path();
                if (path.endsWith("/file")) {
                    handlePostFile(ctx);
                } else if (path.endsWith("/verify")) {
                    handlePostVerify(ctx);
                } else {
                    handlePostHash(ctx);
                }
            }
            default -> ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // region hash

    private void handlePostHash(Context ctx) {
        ctx.contentType("application/json");

        V0HashPostRequest request = ctx.bodyAsClass(V0HashPostRequest.class);

        // An empty string is a legitimate thing to hash; a missing field is not.
        if (request.data() == null) {
            badRequest(ctx, "data is required");
            return;
        }

        HashAlgorithm algorithm;
        byte[] data;
        byte[] key;
        try {
            algorithm = HashAlgorithm.fromWire(request.algorithm());
            data = decode(request.data(), request.encoding(), "data");
            key = decodeKey(request.key(), request.encoding());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        HashService.HashResult result;
        try {
            result = hashService.hash(data, algorithm, key);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Hashing failed", e);
            serverError(ctx, "Hashing failed");
            return;
        }

        ctx.json(new V0HashResponse(result.algorithm().wireName(), result.hash(), result.bytes()));
    }

    // endregion

    // region hash file

    private void handlePostFile(Context ctx) {
        ctx.contentType("application/json");

        UploadedFile uploaded = singleUploadedFile(ctx);
        if (uploaded == null) {
            badRequest(ctx, "A single file is required (field name '" + FILE_FIELD + "')");
            return;
        }

        HashAlgorithm algorithm;
        byte[] key;
        try {
            // The file itself is raw bytes, so `encoding` here describes the key only.
            algorithm = HashAlgorithm.fromWire(defaultIfBlank(ctx.formParam("algorithm"), "sha256"));
            key = decodeKey(ctx.formParam("key"), ctx.formParam("encoding"));
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        HashService.HashResult result;
        // Streamed rather than buffered: a large upload never has to fit in memory.
        try (InputStream content = uploaded.content()) {
            result = hashService.hash(content, algorithm, key);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (IOException e) {
            log.error("Failed to read uploaded file for hashing", e);
            serverError(ctx, "Failed to read the uploaded file");
            return;
        } catch (Exception e) {
            log.error("Hashing failed", e);
            serverError(ctx, "Hashing failed");
            return;
        }

        ctx.json(new V0HashFileResponse(
            result.algorithm().wireName(), result.hash(), result.bytes(), uploaded.filename()));
    }

    private static UploadedFile singleUploadedFile(Context ctx) {
        var files = ctx.uploadedFiles(FILE_FIELD);
        return files.size() == 1 ? files.get(0) : null;
    }

    // endregion

    // region verify

    private void handlePostVerify(Context ctx) {
        ctx.contentType("application/json");

        V0HashVerifyRequest request = ctx.bodyAsClass(V0HashVerifyRequest.class);

        if (request.data() == null) {
            badRequest(ctx, "data is required");
            return;
        }

        HashAlgorithm algorithm;
        byte[] data;
        byte[] key;
        try {
            algorithm = HashAlgorithm.fromWire(request.algorithm());
            data = decode(request.data(), request.encoding(), "data");
            key = decodeKey(request.key(), request.encoding());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        }

        HashService.VerifyResult result;
        try {
            result = hashService.verify(data, algorithm, key, request.hash());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Verification failed", e);
            serverError(ctx, "Verification failed");
            return;
        }

        // A digest that does not match is a normal answer, not an error.
        ctx.json(new V0HashVerifyResponse(
            result.valid(), algorithm.wireName(), result.expected(), result.actual().hash()));
    }

    // endregion

    // region encoding

    /**
     * Reads a caller-supplied string as bytes according to {@code encoding}.
     *
     * @param value the string to read
     * @param encoding {@code utf8} (the default), {@code base64} or {@code hex}
     * @param field the field name, so an error says which input was wrong
     * @return the decoded bytes
     */
    private static byte[] decode(String value, String encoding, String field) {
        String name = encoding == null || encoding.isBlank()
            ? "utf8"
            : encoding.strip().toLowerCase(Locale.ROOT).replace("-", "");

        try {
            return switch (name) {
                case "utf8", "text", "plain" -> value.getBytes(StandardCharsets.UTF_8);
                case "base64", "b64" -> Base64.getDecoder().decode(value);
                case "hex", "base16" -> HexFormat.of().parseHex(value);
                default -> throw new IllegalArgumentException(
                    "unsupported encoding '" + encoding + "' — supported: utf8, base64, hex");
            };
        } catch (IllegalArgumentException e) {
            // Rethrow an unsupported-encoding complaint as it is; a decode
            // failure gets the field name attached so the caller knows which.
            if (e.getMessage() != null && e.getMessage().startsWith("unsupported encoding")) {
                throw e;
            }
            throw new IllegalArgumentException(field + " is not valid " + name);
        }
    }

    /** Same as {@link #decode}, but an absent key stays absent rather than becoming empty bytes. */
    private static byte[] decodeKey(String key, String encoding) {
        return key == null || key.isEmpty() ? null : decode(key, encoding, "key");
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // endregion

    private static void badRequest(Context ctx, String message) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.json(ErrorResponse.of(message));
    }

    private static void serverError(Context ctx, String message) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.json(ErrorResponse.of(message));
    }
}
