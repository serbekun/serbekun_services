/** Record for updating an existing link via PUT request. */
package com.serbekun.ss.domain.dto.http.links;

public record V0LinksPutRequest(
    String uuid,
    String token,
    String url,
    String name,
    String description
) {}
