package com.serbekun.ss.domain.dto.http.linksrepo;

import java.util.List;
import java.util.UUID;

import com.serbekun.ss.domain.models.LinkRepository;

/** Response DTO for a link repository — the token is intentionally excluded. */
public record V0RepositoryGetResponse(UUID repositoryId, String name, String createdAt, List<V0LinkResponse> links) {

    /**
     * Maps a domain repository to its response representation.
     * @param repo the domain repository
     * @return the response DTO
     */
    public static V0RepositoryGetResponse from(LinkRepository repo) {
        return new V0RepositoryGetResponse(
                repo.repositoryId(),
                repo.name(),
                repo.createdAt(),
                repo.links().values().stream().map(V0LinkResponse::from).toList());
    }
}
