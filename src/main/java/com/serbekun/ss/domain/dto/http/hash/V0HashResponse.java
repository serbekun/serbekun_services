/** Response DTO carrying a computed digest. */
package com.serbekun.ss.domain.dto.http.hash;

public record V0HashResponse(String algorithm, String hash, long bytes) {}
