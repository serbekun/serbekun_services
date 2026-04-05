package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.http.handles.v0.dto.cipher.aes.*;
import com.serbekun.ss.service.http.handles.v0.ApiV0CipherAes;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

public class V0CipherAesHttp {
    
    private ApiV0CipherAes v0ApiCipherAes;

    public V0CipherAesHttp(ApiV0CipherAes v0ApiCipherAes) {
        this.v0ApiCipherAes = v0ApiCipherAes;
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

        String json = v0ApiCipherAes.get();
        if (json == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result(json);
    }

    private void handlePostEncrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherPostEncrypt v0CipherPostEncrypt = ctx.bodyAsClass(V0CipherPostEncrypt.class);

        // check data is valid
        if (v0CipherPostEncrypt.data() == null || v0CipherPostEncrypt.data().isBlank() ||
            v0CipherPostEncrypt.key() == null || v0CipherPostEncrypt.key().isBlank()) {
            
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

    
        String json = v0ApiCipherAes.postEncrypt(v0CipherPostEncrypt.data(), v0CipherPostEncrypt.key());
        if (json == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }      
        
        ctx.result(json);
    }

    private void handlePostDecrypt(Context ctx) {
        ctx.contentType("application/json");

        V0CipherPostDecrypt v0CipherPostDecrypt = ctx.bodyAsClass(V0CipherPostDecrypt.class);

        // check data is valid
        if (v0CipherPostDecrypt.data() == null || v0CipherPostDecrypt.data().isBlank() ||
            v0CipherPostDecrypt.key() == null || v0CipherPostDecrypt.key().isBlank()) {
            
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        String json = v0ApiCipherAes.postDecrypt(v0CipherPostDecrypt.data(), v0CipherPostDecrypt.key());
        if (json == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result(json);
    }

}
