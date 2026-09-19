package com.serbekun.ss.domain.dto.http.burnlink;

/**
 * Response for {@code POST /api/v0/burn/{id}/reveal}, carrying the secret that
 * was burned by this call.
 */
public record V0BurnLinkRevealResponse(String text) {
}
