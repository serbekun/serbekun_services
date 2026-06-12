package com.serbekun.ss.http.handles.v0;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.http.handles.v0.dto.cipher.aes.*;
import com.serbekun.ss.service.cipher.CipherService;

public class ApiV0CipherAesHttp {
    
    private final CipherService cipherService;

    public ApiV0CipherAesHttp(CipherService cipherService) {
        this.cipherService = cipherService;
    }

    public void main(Context ctx) {

        HandlerType handlerType = ctx.method();

        switch (handlerType) {
            case GET:
                handleGet(ctx);
                break;
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

    private void handleGet(Context ctx) {
        ctx.contentType("application/json");

        String key = cipherService.generateAesKey();
        if (key == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result("{\"key\":\"" + key + "\"}");
    }

    private void handlePostEncrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherPostEncrypt v0CipherPostEncrypt = ctx.bodyAsClass(V0CipherPostEncrypt.class);

        if (v0CipherPostEncrypt.data() == null || v0CipherPostEncrypt.data().isBlank() ||
            v0CipherPostEncrypt.key() == null || v0CipherPostEncrypt.key().isBlank()) {
            
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String encrypted;
        try {
            encrypted = cipherService.encrypt(v0CipherPostEncrypt.data(), v0CipherPostEncrypt.key());
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result("{\"data\":\"" + encrypted + "\"}");
    }

    private void handlePostDecrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherPostDecrypt v0CipherPostDecrypt = ctx.bodyAsClass(V0CipherPostDecrypt.class);

        if (v0CipherPostDecrypt.data() == null || v0CipherPostDecrypt.data().isBlank() ||
            v0CipherPostDecrypt.key() == null || v0CipherPostDecrypt.key().isBlank()) {
            
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String decrypted;
        try {
            decrypted = cipherService.decrypt(v0CipherPostDecrypt.data(), v0CipherPostDecrypt.key());
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result("{\"data\":\"" + decrypted + "\"}");
    }

}