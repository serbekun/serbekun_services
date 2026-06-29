package com.serbekun.ss.http.handles.api;

import java.util.UUID;

import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.domain.dto.http.linksrepo.V0RepositoryLinksPostRequest;
import com.serbekun.ss.domain.dto.http.linksrepo.V0RepositoryLinksPutRequest;
import com.serbekun.ss.domain.dto.http.linksrepo.V0RepositoryPostRequest;
import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinkRepository;
import com.serbekun.ss.service.linksrepo.LinkRepositoryService;

public class ApiV0RepositoryLinksHttp {

    private final LinkRepositoryService service;

    public ApiV0RepositoryLinksHttp(LinkRepositoryService service) {
        this.service = service;
    }

    // region HTTP Handlers

    /**
     * Handles HTTP requests for the API v0 repository links endpoint.
     * Depending on the HTTP method and path parameters,
     * it delegates to the appropriate handler method for creating a repository,
     * retrieving a repository, adding a link, updating a link, or deleting a link.
     * @param ctx the Javalin HTTP context containing request and response information
     */
    public void handle(Context ctx) {
        HandlerType method = ctx.method();
        switch (method) {
            case POST:
                if (ctx.pathParamMap().containsKey("repositoryId")) {
                    handleAddLink(ctx);
                } else {
                    handleCreateRepository(ctx);
                }
                break;
            case GET:
                handleGetRepository(ctx);
                break;
            case PUT:
                handleUpdateLink(ctx);
                break;
            case DELETE:
                if (ctx.pathParamMap().containsKey("uuid")) {
                    handleDeleteLink(ctx);
                } else {
                    handleRemoveRepository(ctx);
                }
                break;
            default:
                ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    /**
     * Handles the creation of a new link repository. It expects a JSON body containing the repository name,
     * and it returns the created repository's details in JSON format,
     * including the repositoryId, token, name, and createdAt timestamp.
     * @param ctx the Javalin HTTP context containing request and response information
     */
    private void handleCreateRepository(Context ctx) {
        ctx.contentType("application/json");
        V0RepositoryPostRequest body = parseBody(ctx, V0RepositoryPostRequest.class);
        String name = (body != null) ? body.name() : null;
        LinkRepository repo = service.createRepository(name);
        ctx.status(HttpStatus.CREATED);
        ctx.result("{\"repositoryId\":\"" + repo.repositoryId()
            + "\",\"token\":\"" + repo.token()
            + "\",\"name\":\"" + escapeJson(repo.name())
            + "\",\"createdAt\":\"" + repo.createdAt() + "\"}");
    }

    /**
     * Handles the removal of a link repository by its repositoryId and token.
     * @param ctx the Javalin HTTP context containing request and response information
     */
    public void handleRemoveRepository(Context ctx) {
        ctx.contentType("application/json");
        UUID repoId = uuidParam(ctx, "repositoryId");
        String token = ctx.queryParam("token");
        if (repoId == null || token == null || token.isBlank()) {
            writeInvalidRequest(ctx);
            return;
        }
        int status = service.removeRepository(repoId, token);
        switch (status) {
            case 200 -> ctx.status(HttpStatus.NO_CONTENT);
            default -> {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.result("{\"error\":\"NOT_FOUND\",\"message\":\"Repository not found or token is invalid\"}");
            }
        }
    }

    /**
     * Handles the retrieval of a link repository by its repositoryId and token.
     * @param ctx the Javalin HTTP context containing request and response information
     * @throws IllegalArgumentException if the repositoryId or token is missing or invalid
     */
    private void handleGetRepository(Context ctx) {
        ctx.contentType("application/json");
        UUID repoId = uuidParam(ctx, "repositoryId");
        String token = ctx.queryParam("token");
        if (repoId == null || token == null || token.isBlank()) {
            writeInvalidRequest(ctx);
            return;
        }
        LinkRepository repo = service.getRepository(repoId, token);
        if (repo == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.result("{\"error\":\"NOT_FOUND\",\"message\":\"Repository not found or token is invalid\"}");
            return;
        }
        StringBuilder json = new StringBuilder();
        json.append("{\"repositoryId\":\"").append(repo.repositoryId())
            .append("\",\"name\":\"").append(escapeJson(repo.name()))
            .append("\",\"createdAt\":\"").append(repo.createdAt())
            .append("\",\"links\":[");
        boolean first = true;
        for (Link link : repo.links().values()) {
            if (!first) json.append(",");
            first = false;
            json.append("{\"uuid\":\"").append(link.uuid())
                .append("\",\"url\":\"").append(escapeJson(link.url()))
                .append("\",\"name\":\"").append(escapeJson(link.name() != null ? link.name() : ""))
                .append("\",\"description\":\"").append(escapeJson(link.description() != null ? link.description() : ""))
                .append("\"}");
        }
        json.append("]}");
        ctx.result(json.toString());
    }

    /**
     * Handles the addition of a new link to an existing repository. It expects a JSON body containing the link's URL,
     * name, and description, and it returns the created link's details in JSON format,
     * including the link's UUID, URL, name, and description.
     * @param ctx the Javalin HTTP context containing request and response information
     * @throws IllegalArgumentException if the repositoryId, token, or request body is missing or invalid
     */
    private void handleAddLink(Context ctx) {
        ctx.contentType("application/json");
        UUID repoId = uuidParam(ctx, "repositoryId");
        String token = ctx.queryParam("token");
        V0RepositoryLinksPostRequest body = parseBody(ctx, V0RepositoryLinksPostRequest.class);
        if (repoId == null || token == null || token.isBlank() || body == null || body.url() == null || body.url().isBlank()) {
            writeInvalidRequest(ctx);
            return;
        }
        Link link = service.addLink(repoId, token, body.url(), body.name(), body.description());
        if (link == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.result("{\"error\":\"NOT_FOUND\",\"message\":\"Repository not found or token is invalid\"}");
            return;
        }
        ctx.status(HttpStatus.CREATED);
        ctx.result("{\"uuid\":\"" + link.uuid()
            + "\",\"url\":\"" + escapeJson(link.url())
            + "\",\"name\":\"" + escapeJson(link.name() != null ? link.name() : "")
            + "\",\"description\":\"" + escapeJson(link.description() != null ? link.description() : "")
            + "\"}");
    }

    /**
     * Handles the update of an existing link in a repository. It expects a JSON body containing the updated URL,
     * name, and description of the link, and it returns a status code indicating the result
     * @param ctx the Javalin HTTP context containing request and response information
     * @throws IllegalArgumentException if the repositoryId, linkId, token, or request body is missing or invalid
     */
    private void handleUpdateLink(Context ctx) {
        ctx.contentType("application/json");
        UUID repoId = uuidParam(ctx, "repositoryId");
        UUID linkId = uuidParam(ctx, "uuid");
        String token = ctx.queryParam("token");
        V0RepositoryLinksPutRequest body = parseBody(ctx, V0RepositoryLinksPutRequest.class);
        if (repoId == null || linkId == null || token == null || token.isBlank() || body == null) {
            writeInvalidRequest(ctx);
            return;
        }
        int status = service.updateLink(repoId, token, linkId, body.url(), body.name(), body.description());
        writeLinkStatus(ctx, status);
    }

    /**
     * Handles the deletion of an existing link from a repository.
     * It expects the repositoryId, linkId, and token as path parameters and query parameters,
     * and it returns a status code indicating the result.
     * @param ctx the Javalin HTTP context containing request and response information
     */
    private void handleDeleteLink(Context ctx) {
        ctx.contentType("application/json");
        UUID repoId = uuidParam(ctx, "repositoryId");
        UUID linkId = uuidParam(ctx, "uuid");
        String token = ctx.queryParam("token");
        if (repoId == null || linkId == null || token == null || token.isBlank()) {
            writeInvalidRequest(ctx);
            return;
        }
        int status = service.deleteLink(repoId, token, linkId);
        writeLinkStatus(ctx, status);
    }

    // endregion HTTP Handlers

    // region Helper Methods

    /**
     * Writes an appropriate HTTP response based on the status code returned from the service layer.
     * @param ctx the Javalin HTTP context containing request and response information
     * @param status the status code returned from the service layer indicating the result of the operation
     *               (e.g., 404 for not found, 403 for forbidden, 400 for bad request, or 204 for no content)
     */
    private void writeLinkStatus(Context ctx, int status) {
        switch (status) {
            case 200 -> ctx.status(HttpStatus.NO_CONTENT);
            case 400 -> {
                ctx.status(HttpStatus.BAD_REQUEST);
                ctx.result("{\"error\":\"INVALID_REQUEST\",\"message\":\"Required fields are missing\"}");
            }
            default -> {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.result("{\"error\":\"NOT_FOUND\",\"message\":\"Repository or link not found\"}");
            }
        }
    }

    /**
     * Parses the request body of the HTTP context into an instance of the specified class.
     * @param <T> the type of the class to parse the request body into 
     * @param ctx the Javalin HTTP context containing request and response information
     * @param clazz the class to parse the request body into
     * @return an instance of the specified class if parsing is successful, or null if parsing fails
     */
    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves a UUID parameter from the path parameters of the HTTP context.
     * @param ctx the Javalin HTTP context containing request and response information
     * @param name the name of the path parameter to retrieve
     * @return the UUID value of the path parameter if it exists and is valid, or null if it does not exist or is invalid
     */
    private UUID uuidParam(Context ctx, String name) {
        if (ctx.pathParamMap().containsKey(name)) {
            try {
                return UUID.fromString(ctx.pathParamMap().get(name));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Writes a standardized error response for invalid requests to the HTTP context.
     * @param ctx the Javalin HTTP context containing request and response information
     * @throws IllegalArgumentException if the context is null
     */
    private void writeInvalidRequest(Context ctx) {
        ctx.status(HttpStatus.BAD_REQUEST);
        ctx.result("{\"error\":\"INVALID_REQUEST\",\"message\":\"Required fields are missing\"}");
    }

    /**
     * Escapes special characters in a string for safe inclusion in JSON.
     * @param value the string value to escape
     * @return the escaped string, or an empty string if the input value is null
     */
    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    // endregion Helper Methods
}
