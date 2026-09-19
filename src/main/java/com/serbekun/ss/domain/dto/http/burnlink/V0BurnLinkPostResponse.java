package com.serbekun.ss.domain.dto.http.burnlink;

import java.util.List;

/**
 * Response for a created burn link. The {@code token} is shown only here and is
 * required to delete the link early.
 */
public record V0BurnLinkPostResponse(
        String id,
        String token,
        String url,
        long expiredTime,
        List<String> devices,
        List<String> browsers,
        List<String> ipWhitelist,
        List<String> ipBlacklist) {
}
