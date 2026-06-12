package com.serbekun.ss.http.handles.v0;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.http.handles.v0.dto.links.V0LinksDeleteRequest;
import com.serbekun.ss.http.handles.v0.dto.links.V0LinksPostRequest;
import com.serbekun.ss.http.handles.v0.dto.links.V0LinksPutRequest;
import com.serbekun.ss.service.links.LinksService;

public class ApiV0CatalogsLinksHttp {
    
    private final LinksService linksService;

    public ApiV0CatalogsLinksHttp(LinksService linksService) {
        this.linksService = linksService;
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

        String json = linksService.getAllLinksAsJson();
        if (json == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result(json);
    }

    private void handlePost(Context ctx) {
        ctx.contentType("application/json");

        V0LinksPostRequest body = parseBody(ctx, V0LinksPostRequest.class);
        if (body == null || isBlank(body.url())) {
            writeInvalidRequest(ctx);
            return;
        }

        String token = linksService.createLink(body.url(), body.name(), body.description());
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
            || isBlank(body.url())) {
            writeInvalidRequest(ctx);
            return;
        }

        int status = linksService.updateLink(uuid, body.token(), body.url(), body.name(), body.description());
        if (status == 404) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        if (status == 403) {
            ctx.status(HttpStatus.FORBIDDEN);
            return;
        }
        if (status == 400) {
            ctx.status(HttpStatus.BAD_REQUEST);
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

        int status = linksService.deleteLink(uuid, token);
        if (status == 404) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }
        if (status == 403) {
            ctx.status(HttpStatus.FORBIDDEN);
            return;
        }
        if (status == 400) {
            ctx.status(HttpStatus.BAD_REQUEST);
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
        ctx.result("{\"error\":\"INVALID_REQUEST\",\"message\":\"required field \"url\" are missing\"}");
    }
}