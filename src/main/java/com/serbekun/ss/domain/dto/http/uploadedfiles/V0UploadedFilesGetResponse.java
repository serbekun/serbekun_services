/** Response DTO for single file metadata — token is intentionally excluded. */
package com.serbekun.ss.domain.dto.http.uploadedfiles;

import java.util.UUID;

public record V0UploadedFilesGetResponse(UUID uuid, String name, long expiredTime) {}
