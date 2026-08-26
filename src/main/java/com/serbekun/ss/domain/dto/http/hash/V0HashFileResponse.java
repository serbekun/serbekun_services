/** Response DTO carrying the digest of an uploaded file. */
package com.serbekun.ss.domain.dto.http.hash;

public record V0HashFileResponse(String algorithm, String hash, long bytes, String name) {}
