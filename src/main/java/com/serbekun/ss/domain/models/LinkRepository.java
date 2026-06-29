package com.serbekun.ss.domain.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a repository of links, identified by a unique repositoryId and associated with a token.
 * The repository has a name, a creation timestamp, and a collection of links identified by their UUIDs.
 */
public class LinkRepository {

    private final UUID repositoryId;
    private final String token;
    private final String name;
    private final String createdAt;
    private final Map<UUID, Link> links;

    @JsonCreator
    public LinkRepository(
            @JsonProperty("repositoryId") UUID repositoryId,
            @JsonProperty("token") String token,
            @JsonProperty("name") String name,
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("links") Map<UUID, Link> links
    ) {
        if (repositoryId == null) throw new IllegalArgumentException("repositoryId is required");
        if (token == null || token.isBlank()) throw new IllegalArgumentException("token is required");
        this.repositoryId = repositoryId;
        this.token = token;
        this.name = name;
        this.createdAt = createdAt;
        this.links = new HashMap<>();
        if (links != null) {
            this.links.putAll(links);
        }
    }

    @JsonProperty("repositoryId")
    public UUID repositoryId() { return repositoryId; }

    @JsonProperty("token")
    public String token() { return token; }

    @JsonProperty("name")
    public String name() { return name; }

    @JsonProperty("createdAt")
    public String createdAt() { return createdAt; }

    @JsonProperty("links")
    public Map<UUID, Link> links() { return links; }

    @Override
    public boolean equals(Object o) {
        return (o instanceof LinkRepository) && repositoryId.equals(((LinkRepository) o).repositoryId);
    }

    @Override
    public int hashCode() {
        return repositoryId.hashCode();
    }
}
