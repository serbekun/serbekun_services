/** Response DTO carrying the outcome of an integrity check. */
package com.serbekun.ss.domain.dto.http.hash;

public record V0HashVerifyResponse(boolean valid, String algorithm, String expected, String actual) {}
