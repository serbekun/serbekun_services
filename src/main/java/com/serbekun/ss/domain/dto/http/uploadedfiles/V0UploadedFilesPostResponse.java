/** Response DTO returned after a successful file upload. */
package com.serbekun.ss.domain.dto.http.uploadedfiles;

import java.util.UUID;

public record V0UploadedFilesPostResponse(UUID uuid, String token, String name, long expiredTime) {}
