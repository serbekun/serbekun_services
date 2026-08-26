package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.cipher.hybrid.*;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.cipher.HybridService;

/**
 * HTTP handler for hybrid encryption — the way to protect a payload larger
 * than an RSA key: AES-GCM does the bulk work and RSA-OAEP only wraps the
 * single-use AES key.
 */
public class ApiV0CipherHybridHttp {

    private final CipherService cipherService;

    public ApiV0CipherHybridHttp(CipherService cipherService) {
        this.cipherService = cipherService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        HandlerType handlerType = ctx.method();

        switch (handlerType) {
            case POST:

                String path = ctx.path();
                if (path.endsWith("/encrypt")) {
                    handlePostEncrypt(ctx);
                } else if (path.endsWith("/decrypt")) {
                    handlePostDecrypt(ctx);
                } else {
                    ctx.status(HttpStatus.NOT_FOUND);
                }

                break;
            default:
                ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
                break;
        }
    }

    private void handlePostEncrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherHybridPostEncrypt request = ctx.bodyAsClass(V0CipherHybridPostEncrypt.class);

        if (isBlank(request.data()) || isBlank(request.publicKey())) {
            badRequest(ctx, "data and publicKey are required");
            return;
        }

        HybridService.HybridPayload payload;
        try {
            payload = cipherService.encryptHybrid(request.data(), request.publicKey());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, reason(e, "data or publicKey is not valid base64"));
            return;
        } catch (Exception e) {
            serverError(ctx, "Encryption failed");
            return;
        }

        ctx.json(new V0CipherHybridEncryptResponse(payload.data(), payload.encryptedKey()));
    }

    private void handlePostDecrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherHybridPostDecrypt request = ctx.bodyAsClass(V0CipherHybridPostDecrypt.class);

        if (isBlank(request.data()) || isBlank(request.encryptedKey()) || isBlank(request.privateKey())) {
            badRequest(ctx, "data, encryptedKey and privateKey are required");
            return;
        }

        String decrypted;
        try {
            decrypted = cipherService.decryptHybrid(request.data(), request.encryptedKey(), request.privateKey());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, reason(e, "data, encryptedKey or privateKey is not valid base64"));
            return;
        } catch (Exception e) {
            serverError(ctx, "Decryption failed");
            return;
        }

        ctx.json(new V0CipherHybridDataResponse(decrypted));
    }

    /**
     * Prefers the message the crypto layer produced — "Invalid RSA public key"
     * or the exact Base64 problem — over a guess about which field is at fault.
     */
    private static String reason(IllegalArgumentException e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void badRequest(Context ctx, String message) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.json(ErrorResponse.of(message));
    }

    private static void serverError(Context ctx, String message) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.json(ErrorResponse.of(message));
    }
}
