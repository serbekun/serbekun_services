/** Response DTO for single file metadata — token is intentionally excluded. */
package com.serbekun.ss.http.handles.v0.dto.uploadedfiles;

import java.util.UUID;

public record V0UploadedFilesGetResponse(UUID uuid, String name, long expiredTime) {}
