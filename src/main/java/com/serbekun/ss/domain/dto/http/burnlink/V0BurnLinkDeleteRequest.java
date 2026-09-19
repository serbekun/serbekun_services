package com.serbekun.ss.domain.dto.http.burnlink;

/**
 * Optional request body for {@code DELETE /api/v0/burn/{id}} when the token is
 * not supplied as a {@code ?token=} query param.
 */
public record V0BurnLinkDeleteRequest(String token) {
}
