/** Request DTO for file upload — metadata fields accompanying the binary file. */
package com.serbekun.ss.http.handles.v0.dto.uploadedfiles;

public record V0UploadedFilesPostRequest(String name, long ttl) {}
