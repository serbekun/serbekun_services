package com.serbekun.ss.http.handles.api;

import java.io.IOException;
import java.util.UUID;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.domain.dto.http.uploadedfiles.V0UploadedFilesDeleteRequest;
import com.serbekun.ss.domain.dto.http.uploadedfiles.V0UploadedFilesGetResponse;
import com.serbekun.ss.domain.dto.http.uploadedfiles.V0UploadedFilesPostResponse;
import com.serbekun.ss.service.uploadedfiles.UploadedFilesService;

/**
 * HTTP handler for uploaded files — upload, download, list, delete.
 */
public class ApiV0UploadedFilesHttp {

    // region fields

    /** Logger instance */
    private static final Logger log = LoggerFactory.getLogger(ApiV0UploadedFilesHttp.class);
    
    /** Object mapper instance */
    private static final ObjectMapper mapper = new ObjectMapper();

    /** Uploaded files service instance */
    private final UploadedFilesService service;

    // endregion

    public ApiV0UploadedFilesHttp(UploadedFilesService service) {
        this.service = service;
    }

    // region main handler

    /** Main handler for the API endpoint */
    public void main(Context ctx) {
        switch (ctx.method()) {
            case GET    -> handleGet(ctx);
            case POST   -> handlePost(ctx);
            case DELETE -> handleDelete(ctx);
            default     -> ctx.status(HttpStatus.METHOD_NOT_ALLOWED);
        }
    }

    // region get

    /**
     * Handles GET requests for uploaded files.
     * <p>
     * If the request is to list all files (no UUID in path), it returns a 403 Forbidden response.
     * If the request is to get a specific file's metadata or download the file, it verifies access using the provided token.
     * If access is granted, it either returns the file metadata or the file content for download.
     * If access is denied, it returns a 403 Forbidden response or a 404 Not Found response if the file does not exist.
     * @param ctx
     */
    private void handleGet(Context ctx) {
        String uuidStr = pathParam(ctx, "uuid");

        if (uuidStr == null) {
            // Listing all files is forbidden
            ctx.contentType("application/json");
            ctx.status(HttpStatus.FORBIDDEN);
            ctx.result("{\"error\":\"Listing all files is not allowed\"}");
            return;
        }

        UUID uuid = parseUuid(ctx, uuidStr);
        if (uuid == null) return;

        String token = ctx.queryParam("token");
        com.serbekun.ss.domain.models.UploadedFile f = service.verifyFileAccess(uuid, token);
        if (f == null) {
            writeAccessDenied(ctx, uuid);
            return;
        }

        if (ctx.path().endsWith("/download")) {
            handleDownload(ctx, uuid, f);
        } else {
            handleGetOne(ctx, f);
        }
    }

    /**
     * Handles GET request for a single file's metadata.
     * <p>
     * If the file is found and access is granted, it returns the file metadata in JSON format.
     * If serialization fails, it returns a 500 Internal Server Error response.
     * @param ctx
     * @param f
     */
    private void handleGetOne(Context ctx, com.serbekun.ss.domain.models.UploadedFile f) {
        ctx.contentType("application/json");
        try {
            var response = new V0UploadedFilesGetResponse(f.uuid(), f.name(), f.expiredTime());
            ctx.result(mapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to serialize uploaded file {}", f.uuid(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles GET request for downloading a file.
     * <p>
     * If the file is found and access is granted, it returns the file content for download.
     * If the file is not found, it returns a 404 Not Found response.
     * If an error occurs while reading the file, it returns a 500 Internal Server Error response.
     * @param ctx
     * @param uuid
     * @param meta
     */
    private void handleDownload(Context ctx, UUID uuid,
                                com.serbekun.ss.domain.models.UploadedFile meta) {
        byte[] content;
        try {
            content = service.getFileContent(uuid);
        } catch (IOException e) {
            log.error("Failed to read file content for uuid={}", uuid, e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            return;
        }

        if (content == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.result("{\"error\":\"File data not found on disk\"}");
            return;
        }

        ctx.contentType("application/octet-stream");
        ctx.header("Content-Disposition", "attachment; filename=\"" + meta.name() + "\"");
        ctx.result(content);
    }

    // endregion

    // region post

    /**
     * Handles POST requests for uploading a file.
     * <p>
     * It expects a single file upload with the field name 'file'.
     * It also accepts optional form parameters 'name' for the file name and 'ttl' for the time-to-live in seconds.
     * If the upload is successful, it returns a 201 Created response with the file's UUID, token, name, and expiration time in JSON format.
     * If the upload fails due to missing file, invalid parameters,
     * or server errors, it returns appropriate error 
     * responses with relevant status codes and messages.
     * @param ctx
     */
    private void handlePost(Context ctx) {
        ctx.contentType("application/json");

        UploadedFile uploaded = getSingleUploadedFile(ctx);
        if (uploaded == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"A single file is required (field name 'file')\"}");
            return;
        }

        String name = ctx.formParam("name");
        if (name == null || name.isBlank()) {
            name = uploaded.filename();
        }

        long ttlSeconds = parseLongParam(ctx, "ttl", 0);
        long expiredTime = ttlSeconds > 0
                ? System.currentTimeMillis() + ttlSeconds * 1000L
                : 0;

        UUID uuid = UUID.randomUUID();
        String token = UUID.randomUUID().toString();

        com.serbekun.ss.domain.models.UploadedFile meta =
                new com.serbekun.ss.domain.models.UploadedFile(uuid, name, token, expiredTime);

        try {
            service.uploadFile(meta, uploaded.content().readAllBytes());
        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\":\"Failed to store file\"}");
            return;
        }

        ctx.status(HttpStatus.CREATED);
        try {
            ctx.result(mapper.writeValueAsString(
                    new V0UploadedFilesPostResponse(uuid, token, name, expiredTime)));
        } catch (Exception e) {
            log.error("Failed to serialize upload response", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // endregion

    // region delete

    /**
     * Handles DELETE requests for deleting a file.
     * <p>
     * It expects the file's UUID as a path parameter and a valid token either as a query parameter or in the request body.
     * If the deletion is successful, it returns a 204 No Content response.
     * If the file is not found or the token is invalid, it returns appropriate error responses with relevant status codes and messages.
     * @param ctx
     */
    private void handleDelete(Context ctx) {
        ctx.contentType("application/json");

        String uuidStr = pathParam(ctx, "uuid");
        if (uuidStr == null) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"UUID path parameter is required\"}");
            return;
        }

        UUID uuid = parseUuid(ctx, uuidStr);
        if (uuid == null) return;

        // Token from query param or request body
        String token = ctx.queryParam("token");
        if (token == null) {
            V0UploadedFilesDeleteRequest body = parseBody(ctx, V0UploadedFilesDeleteRequest.class);
            if (body != null) {
                token = body.token();
            }
        }

        int status = service.deleteFile(uuid, token);
        switch (status) {
            case 404 -> {
                ctx.status(HttpStatus.NOT_FOUND);
                ctx.result("{\"error\":\"File not found\"}");
            }
            case 403 -> {
                ctx.status(HttpStatus.FORBIDDEN);
                ctx.result("{\"error\":\"Invalid token\"}");
            }
            case 500 -> {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                ctx.result("{\"error\":\"Failed to delete file from disk\"}");
            }
            default -> ctx.status(HttpStatus.NO_CONTENT);
        }
    }

    // endregion

    // region helpers

    /**
     * Parses a UUID from a string.
     * @param ctx the HTTP context
     * @param uuidStr the UUID string
     * @return the parsed UUID or null if invalid
     */
    private UUID parseUuid(Context ctx, String uuidStr) {
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\":\"Invalid UUID format\"}");
            return null;
        }
    }

    /**
     * Writes an access denied response.
     * @param ctx the HTTP context
     * @param uuid the file UUID
     */
    private void writeAccessDenied(Context ctx, UUID uuid) {
        ctx.contentType("application/json");
        if (!service.exists(uuid)) {
            ctx.status(HttpStatus.NOT_FOUND);
            ctx.result("{\"error\":\"File not found\"}");
        } else {
            ctx.status(HttpStatus.FORBIDDEN);
            ctx.result("{\"error\":\"Invalid or missing token\"}");
        }
    }

    /**
     * Parses a long integer from a form parameter.
     * @param ctx the HTTP context
     * @param name the parameter name
     * @param defaultValue the default value
     * @return the parsed long integer or the default value if invalid
     */
    private long parseLongParam(Context ctx, String name, long defaultValue) {
        String val = ctx.formParam(name);
        if (val == null || val.isBlank()) return defaultValue;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets the first uploaded file from the HTTP context.
     * @param ctx the HTTP context
     * @return the first uploaded file or null if none found
     */
    private UploadedFile getSingleUploadedFile(Context ctx) {
        var files = ctx.uploadedFiles("file");
        if (files == null || files.isEmpty()) return null;
        return files.get(0);
    }

    /**
     * Parses the request body into an object of the specified class.
     * @param ctx the HTTP context
     * @param clazz the class to parse into
     * @return the parsed object or null if parsing fails
     */
    private <T> T parseBody(Context ctx, Class<T> clazz) {
        try {
            return ctx.bodyAsClass(clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Retrieves a path parameter from the HTTP context.
     * @param ctx the HTTP context
     * @param name the parameter name
     * @return the parameter value or null if not found
     */
    private String pathParam(Context ctx, String name) {
        if (ctx.pathParamMap().containsKey(name)) {
            return ctx.pathParamMap().get(name);
        }
        return null;
    }

    // endregion
    // endregion
}
