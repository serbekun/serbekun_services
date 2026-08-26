package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import javax.crypto.IllegalBlockSizeException;

import com.serbekun.ss.domain.dto.http.ErrorResponse;
import com.serbekun.ss.domain.dto.http.cipher.rsa.*;
import com.serbekun.ss.service.cipher.CipherService;
import com.serbekun.ss.service.cipher.RsaService;

/**
 * HTTP handler for the RSA endpoints — key pair generation, encryption,
 * decryption, signing and signature verification.
 * <p>
 * Every payload travels as Base64 and nothing is stored on the server: the
 * private key is returned once at generation time and must be sent back with
 * each call that needs it.
 */
public class ApiV0CipherRsaHttp {

    private final CipherService cipherService;

    public ApiV0CipherRsaHttp(CipherService cipherService) {
        this.cipherService = cipherService;
    }

    /** Main handler for the API endpoint. */
    public void main(Context ctx) {
        HandlerType handlerType = ctx.method();

        switch (handlerType) {
            case GET:
                handleGetKeyPair(ctx);
                break;
            case POST:

                String path = ctx.path();
                if (path.endsWith("/encrypt")) {
                    handlePostEncrypt(ctx);
                } else if (path.endsWith("/decrypt")) {
                    handlePostDecrypt(ctx);
                } else if (path.endsWith("/sign")) {
                    handlePostSign(ctx);
                } else if (path.endsWith("/verify")) {
                    handlePostVerify(ctx);
                } else {
                    ctx.status(HttpStatus.NOT_FOUND);
                }

                break;
            default:
                ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
                break;
        }
    }

    private void handleGetKeyPair(Context ctx) {
        ctx.contentType("application/json");

        RsaService.RsaKeyPair keyPair;
        try {
            keyPair = cipherService.generateRsaKeyPair();
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.json(ErrorResponse.of("Key pair generation failed"));
            return;
        }

        ctx.json(new V0CipherRsaKeyPairResponse(keyPair.publicKey(), keyPair.privateKey()));
    }

    private void handlePostEncrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherRsaPostEncrypt request = ctx.bodyAsClass(V0CipherRsaPostEncrypt.class);

        if (isBlank(request.data()) || isBlank(request.publicKey())) {
            badRequest(ctx, "data and publicKey are required");
            return;
        }

        String encrypted;
        try {
            encrypted = cipherService.encryptRsa(request.data(), request.publicKey());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, reason(e, "data or publicKey is not valid base64"));
            return;
        } catch (Exception e) {
            // RSA can only encrypt a payload smaller than the key. That is the
            // caller's mistake, so say so — and point at the way out.
            if (causedByBlockSize(e)) {
                badRequest(ctx, "data is too large for this RSA key — use /api/v0/cipher/hybrid/encrypt");
                return;
            }
            serverError(ctx, "Encryption failed");
            return;
        }

        ctx.json(new V0CipherRsaDataResponse(encrypted));
    }

    private void handlePostDecrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherRsaPostDecrypt request = ctx.bodyAsClass(V0CipherRsaPostDecrypt.class);

        if (isBlank(request.data()) || isBlank(request.privateKey())) {
            badRequest(ctx, "data and privateKey are required");
            return;
        }

        String decrypted;
        try {
            decrypted = cipherService.decryptRsa(request.data(), request.privateKey());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, reason(e, "data or privateKey is not valid base64"));
            return;
        } catch (Exception e) {
            serverError(ctx, "Decryption failed");
            return;
        }

        ctx.json(new V0CipherRsaDataResponse(decrypted));
    }

    private void handlePostSign(Context ctx) {
        ctx.contentType("application/json");

        V0CipherRsaPostSign request = ctx.bodyAsClass(V0CipherRsaPostSign.class);

        if (isBlank(request.data()) || isBlank(request.privateKey())) {
            badRequest(ctx, "data and privateKey are required");
            return;
        }

        String signature;
        try {
            signature = cipherService.signRsa(request.data(), request.privateKey());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, reason(e, "data or privateKey is not valid base64"));
            return;
        } catch (Exception e) {
            serverError(ctx, "Signing failed");
            return;
        }

        ctx.json(new V0CipherRsaSignatureResponse(signature));
    }

    private void handlePostVerify(Context ctx) {
        ctx.contentType("application/json");

        V0CipherRsaPostVerify request = ctx.bodyAsClass(V0CipherRsaPostVerify.class);

        if (isBlank(request.data()) || isBlank(request.signature()) || isBlank(request.publicKey())) {
            badRequest(ctx, "data, signature and publicKey are required");
            return;
        }

        boolean valid;
        try {
            valid = cipherService.verifyRsa(request.data(), request.signature(), request.publicKey());
        } catch (IllegalArgumentException e) {
            badRequest(ctx, reason(e, "data, signature or publicKey is not valid base64"));
            return;
        } catch (Exception e) {
            serverError(ctx, "Verification failed");
            return;
        }

        // A mismatching signature is a normal answer, not an error.
        ctx.json(new V0CipherRsaVerifyResponse(valid));
    }

    /**
     * Prefers the message the crypto layer produced — "Invalid RSA public key"
     * or the exact Base64 problem — over a guess about which field is at fault.
     */
    private static String reason(IllegalArgumentException e, String fallback) {
        return e.getMessage() == null || e.getMessage().isBlank() ? fallback : e.getMessage();
    }

    private static boolean causedByBlockSize(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof IllegalBlockSizeException) {
                return true;
            }
        }
        return false;
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
