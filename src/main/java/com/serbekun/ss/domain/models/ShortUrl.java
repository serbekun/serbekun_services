package com.serbekun.ss.domain.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a shortened URL in the system.
 * <p>
 * The {@code id} is a short, public code used to resolve the redirect.
 * The {@code token} is a secret issued at creation time and required to
 * delete the record. {@code name} and {@code description} are optional
 * metadata. This model is unrelated to {@link Link}.
 */
public class ShortUrl {

    private final String id;
    private final String targetUrl;
    private final String token;
    private final String name;
    private final String description;
    private final long createdTime;

    @JsonCreator
    public ShortUrl(
            @JsonProperty("id") String id,
            @JsonProperty("target_url") String targetUrl,
            @JsonProperty("token") String token,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("created_time") long createdTime
        ) {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
            if (targetUrl == null || targetUrl.isBlank()) throw new IllegalArgumentException("target_url is required");
            this.id = id;
            this.targetUrl = targetUrl;
            this.token = token;
            this.name = name;
            this.description = description;
            this.createdTime = createdTime;
        }

        @JsonProperty("id")
        public String id() { return id; }

        @JsonProperty("target_url")
        public String targetUrl() { return targetUrl; }

        @JsonProperty("token")
        public String token() { return token; }

        @JsonProperty("name")
        public String name() { return name; }

        @JsonProperty("description")
        public String description() { return description; }

        @JsonProperty("created_time")
        public long createdTime() { return createdTime; }

        @Override public boolean equals(Object o) {
            return (o instanceof ShortUrl) && id.equals(((ShortUrl) o).id);
        }

        @Override public int hashCode() {
            return id.hashCode();
        }
}
