/** Response DTO returned after a successful file upload. */
package com.serbekun.ss.http.handles.v0.dto.uploadedfiles;

import java.util.UUID;

public record V0UploadedFilesPostResponse(UUID uuid, String token, String name, long expiredTime) {}
