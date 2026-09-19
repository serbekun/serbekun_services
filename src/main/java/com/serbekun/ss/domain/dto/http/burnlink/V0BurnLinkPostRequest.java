package com.serbekun.ss.domain.dto.http.burnlink;

import java.util.List;

/**
 * Request body for {@code POST /api/v0/burn}.
 * <p>
 * {@code ttl} is a lifetime in seconds where {@code 0} or absent means "never
 * expires", matching the uploaded-files convention. {@code devices},
 * {@code browsers}, {@code ipWhitelist} and {@code ipBlacklist} are allow/deny
 * lists; empty means no restriction.
 */
public record V0BurnLinkPostRequest(
        String text,
        String name,
        Long ttl,
        List<String> devices,
        List<String> browsers,
        List<String> ipWhitelist,
        List<String> ipBlacklist,
        String baseUrl) {
}
