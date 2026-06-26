/** Request DTO for file upload — metadata fields accompanying the binary file. */
package com.serbekun.ss.domain.dto.http.uploadedfiles;

public record V0UploadedFilesPostRequest(String name, long ttl) {}
