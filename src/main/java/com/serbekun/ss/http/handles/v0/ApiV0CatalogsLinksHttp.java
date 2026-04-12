package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.http.handles.v0.dto.links.V0LinksDeleteRequest;
import com.serbekun.ss.http.handles.v0.dto.links.V0LinksPostRequest;
import com.serbekun.ss.http.handles.v0.dto.links.V0LinksPutRequest;
import com.serbekun.ss.service.http.handles.v0.ApiV0CatalogsLinks;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

// TODO move json string writeing to V0Links

public class ApiV0CatalogsLinksHttp {
    
    private final ApiV0CatalogsLinks apiV0CatalogsLinks;

    public ApiV0CatalogsLinksHttp(ApiV0CatalogsLinks apiV0CatalogsLinks) {
        this.apiV0CatalogsLinks = apiV0CatalogsLinks;
    }

    public void main(Context ctx) {

        HandlerType handlerType = ctx.method();

        switch (handlerType) {
            case GET:
                handleGet(ctx);
                break;
            case POST:
                handlePost(ctx);
                break;
            case PUT:
                handlePut(ctx);
                break;
            case DELETE:
                handleDelete(ctx);
                break;
            default:
                ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
                break;
        }
    }

    private void handleGet(Context ctx) {
        ctx.contentType("application/json");

        String json = apiV0CatalogsLinks.get();
        if (json == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result(json);
    }

    private void handlePost(Context ctx) {
        ctx.contentType("application/json");

        V0LinksPostRequest body = parseBody(ctx, V0LinksPostRequest.class);
        if (body == null || isBlank(body.url()) || isBlank(body.name()) || isBlank(body.description())) {
            writeInvalidRequest(ctx);
            return;
        }

        com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPostRequest request =
            new com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPostRequest(
                body.url(),
                body.name(),
                body.description()
            );

        String token = apiV0CatalogsLinks.post(request);
        ctx.status(HttpStatus.CREATED);
        ctx.result("{\"token\":\"" + token + "\"}");
    }

    private void handlePut(Context ctx) {
        ctx.contentType("application/json");

        V0LinksPutRequest body = parseBody(ctx, V0LinksPutRequest.class);
        String uuid = pathParam(ctx, "uuid");
        if (isBlank(uuid) && body != null) {
            uuid = body.uuid();
        }

        if (body == null || isBlank(uuid) || isBlank(body.token())
            || isBlank(body.url()) || isBlank(body.name()) || isBlank(body.description())) {
            writeInvalidRequest(ctx);
            return;
        }

        com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPutRequest request =
            new com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPutRequest(
                uuid,
                body.token(),
                body.url(),
                body.name(),
                body.description()
            );

        int status = apiV0CatalogsLinks.put(request);
        if (status == 404) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        if (status == 403) {
            ctx.status(HttpStatus.FORBIDDEN);
            return;
        }

        ctx.status(HttpStatus.NO_CONTENT);
    }

    private void handleDelete(Context ctx) {
        ctx.contentType("application/json");

        V0LinksDeleteRequest body = parseBody(ctx, V0LinksDeleteRequest.class);
        String uuid = pathParam(ctx, "uuid");
        String token = body != null ? body.token() : null;
        if (isBlank(uuid) && body != null) {
            uuid = body.uuid();
        }

        if (isBlank(uuid) || isBlank(token)) {
            writeInvalidRequest(ctx);
            return;
        }

        com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksDeleteRequest request =
            new com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksDeleteRequest(
                uuid,
                token
            );

        int status = apiV0CatalogsLinks.delete(request);
        if (status == 404) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        if (status == 403) {
            ctx.status(HttpStatus.FORBIDDEN);
            return;
        }

        ctx.status(HttpStatus.NO_CONTENT);
    }

    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private String pathParam(Context ctx, String name) {
        if (ctx.pathParamMap().containsKey(name)) {
            return ctx.pathParamMap().get(name);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void writeInvalidRequest(Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.result("{\"error\":\"INVALID_REQUEST\",\"message\":\"required fields are missing\"}");
    }
}