package com.serbekun.ss.http.handles.api;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.domain.dto.http.links.V0LinksDeleteRequest;
import com.serbekun.ss.domain.dto.http.links.V0LinksPostRequest;
import com.serbekun.ss.domain.dto.http.links.V0LinksPutRequest;
import com.serbekun.ss.service.links.LinksService;

/**
 * Handles HTTP requests for the API version 0 related to catalogs links.
 * ApiV0CatalogsLinksHttp
 */
public class ApiV0CatalogsLinksHttp {
    
    private final LinksService linksService;

    public ApiV0CatalogsLinksHttp(LinksService linksService) {
        this.linksService = linksService;
    }

    /**
     * Handles the main HTTP request for the API version 0 catalogs links.
     * @param ctx the Javalin context containing the request.
     */
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

    /**
     * Handles the GET request for retrieving all links in JSON format.
     * @param ctx the Javalin context containing the request.
     */
    private void handleGet(Context ctx) {
        ctx.contentType("application/json");

        String json = linksService.getAllLinksAsJson();
        if (json == null) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        ctx.result(json);
    }

    /**
     * Handles the POST request for creating a new link.
     * @param ctx the Javalin context containing the request.
     */
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

    /**
     * Handles the PUT request for updating an existing link.
     * @param ctx the Javalin context containing the request.
     */
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

    /**
     * Handles the DELETE request for deleting an existing link.
     * @param ctx the Javalin context containing the request.
     */
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

    /**
     * Parses the request body into an instance of the specified class.
     * @param <T> the type of the class to parse the body into
     * @param ctx the Javalin context containing the request
     * @param clazz the class to parse the body into
     * @return an instance of the specified class, or null if parsing fails
     */
    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves the value of a path parameter from the context.
     * @param ctx the Javalin context containing the request.
     * @param name name of the path parameter to retrieve.
     * @return the value of the path parameter, or null if not found.
     */
    private String pathParam(Context ctx, String name) {
        if (ctx.pathParamMap().containsKey(name)) {
            return ctx.pathParamMap().get(name);
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Writes an invalid request error response.
     * @param ctx the Javalin context containing the request.
     */
    private void writeInvalidRequest(Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.result("{\"error\":\"INVALID_REQUEST\",\"message\":\"required field \"url\" are missing\"}");
    }
}