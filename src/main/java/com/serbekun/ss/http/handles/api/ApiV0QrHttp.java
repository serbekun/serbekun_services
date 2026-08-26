package com.serbekun.ss.http.handles.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.qr.V0QrGenerateRequest;
import com.serbekun.ss.domain.dto.http.qr.V0QrGenerateResponse;
import com.serbekun.ss.domain.dto.http.qr.V0QrReadRequest;
import com.serbekun.ss.domain.dto.http.qr.V0QrReadResponse;
import com.serbekun.ss.service.qr.QrService;

/**
 * HTTP handler for QR codes — generate one, or read one out of an image.
 * <p>
 * {@code /generate} answers with the raw image by default, so the response can
 * be saved or piped straight to a file. A caller that would rather embed the
 * code asks for {@code application/json} (an explicit {@code Accept} header, or
 * {@code ?json=true}) and gets a {@code data:} URL instead.
 * <p>
 * {@code /read} takes the image whichever way is convenient: a multipart
 * {@code file}, a raw {@code image/*} body, or base64 in JSON. An image that
 * simply holds no code is a 200 with {@code found: false} — not finding one is
 * an answer, not a failure.
 */
public class ApiV0QrHttp {

    private static final Logger log = LoggerFactory.getLogger(ApiV0QrHttp.class);

    /** Multipart field the read endpoint looks in. */
    private static final String FILE_FIELD = "file";

    /** Ceiling on an image posted for reading — well past any real photo of a code. */
    private static final int MAX_IMAGE_BYTES = 16 * 1024 * 1024;

    private final QrService qrService;

    public ApiV0QrHttp(QrService qrService) {
        this.qrService = qrService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        switch (ctx.method()) {
            case POST -> {
                if (ctx.path().endsWith("/read")) {
                    handlePostRead(ctx);
                } else {
                    handlePostGenerate(ctx);
                }
            }
            default -> ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // region generate

    private void handlePostGenerate(Context ctx) {
        V0QrGenerateRequest request;
        try {
            request = ctx.bodyAsClass(V0QrGenerateRequest.class);
        } catch (Exception e) {
            badRequest(ctx, "a JSON body with 'data' is required");
            return;
        }

        if (request == null || request.data() == null || request.data().isEmpty()) {
            badRequest(ctx, "'data' is required");
            return;
        }

        QrService.QrImage image;
        try {
            image = qrService.generate(request.data(), QrService.QrOptions.of(
                request.format(), request.size(), request.errorCorrection(),
                request.foreground(), request.background(), request.margin()));
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("QR generation failed", e);
            serverError(ctx, "QR generation failed");
            return;
        }

        if (wantsJson(ctx)) {
            ctx.contentType("application/json");
            ctx.json(new V0QrGenerateResponse(
                image.format().wireName(), image.contentType(), image.size(), image.dataUrl()));
            return;
        }

        ctx.contentType(image.contentType());
        ctx.header("Content-Disposition", "inline; filename=\"qr." + image.format().wireName() + "\"");
        ctx.result(image.content());
    }

    /**
     * Whether the caller wants the code wrapped in JSON rather than as bytes.
     * <p>
     * Only an explicit mention counts: a browser's {@code Accept: *&#47;*} means
     * "whatever you have", and what this endpoint has by default is an image.
     */
    private static boolean wantsJson(Context ctx) {
        if ("true".equalsIgnoreCase(ctx.queryParam("json"))) {
            return true;
        }
        String accept = ctx.header("Accept");
        return accept != null && accept.toLowerCase(Locale.ROOT).contains("application/json");
    }

    // endregion

    // region read

    private void handlePostRead(Context ctx) {
        ctx.contentType("application/json");

        byte[] image;
        try {
            image = readImage(ctx);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (IOException e) {
            log.error("Failed to read the uploaded image", e);
            serverError(ctx, "Failed to read the uploaded image");
            return;
        }

        QrService.QrRead result;
        try {
            result = qrService.read(image);
        } catch (IllegalArgumentException e) {
            badRequest(ctx, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("QR reading failed", e);
            serverError(ctx, "QR reading failed");
            return;
        }

        // An image without a code is a normal answer, not an error.
        ctx.json(new V0QrReadResponse(result.found(), result.text(), result.format()));
    }

    /**
     * Pulls the image out of whichever shape the request took.
     *
     * @return the image bytes
     * @throws IllegalArgumentException when no image is there, or it is too large
     */
    private static byte[] readImage(Context ctx) throws IOException {
        var uploads = ctx.uploadedFiles(FILE_FIELD);
        if (uploads.size() == 1) {
            UploadedFile uploaded = uploads.get(0);
            if (uploaded.size() > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("the image is too large");
            }
            try (InputStream content = uploaded.content()) {
                return content.readAllBytes();
            }
        }
        if (uploads.size() > 1) {
            throw new IllegalArgumentException("send a single image (field name '" + FILE_FIELD + "')");
        }

        String contentType = ctx.contentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            byte[] body = ctx.bodyAsBytes();
            if (body.length == 0) {
                throw new IllegalArgumentException("an image is required");
            }
            return body;
        }

        V0QrReadRequest request;
        try {
            request = ctx.bodyAsClass(V0QrReadRequest.class);
        } catch (Exception e) {
            request = null;
        }
        if (request == null || request.image() == null || request.image().isBlank()) {
            throw new IllegalArgumentException(
                "an image is required — send multipart '" + FILE_FIELD
                    + "', a raw image/* body, or JSON {\"image\": \"<base64>\"}");
        }

        return decodeBase64(request.image());
    }

    /** Decodes base64, tolerating a {@code data:} URL prefix and the url-safe alphabet. */
    private static byte[] decodeBase64(String value) {
        String payload = value.strip();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma > 0) {
            payload = payload.substring(comma + 1);
        }
        payload = payload.replaceAll("\\s", "");

        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            try {
                return Base64.getUrlDecoder().decode(payload);
            } catch (IllegalArgumentException urlSafe) {
                throw new IllegalArgumentException("'image' is not valid base64");
            }
        }
    }

    // endregion

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
}
