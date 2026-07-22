package com.serbekun.ss.domain.dto.http.linksrepo;

import java.util.UUID;

import com.serbekun.ss.domain.models.Link;

/** Response DTO for a single link inside a repository. */
public record V0LinkResponse(UUID uuid, String url, String name, String description) {

    /**
     * Maps a domain link to its response representation, replacing missing
     * optional fields with an empty string.
     * @param link the domain link
     * @return the response DTO
     */
    public static V0LinkResponse from(Link link) {
        return new V0LinkResponse(
                link.uuid(),
                link.url(),
                link.name() != null ? link.name() : "",
                link.description() != null ? link.description() : "");
    }
}
