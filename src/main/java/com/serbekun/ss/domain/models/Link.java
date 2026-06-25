package com.serbekun.ss.domain.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a link with a unique identifier, URL, name, and description.
 */
public class Link {

    // region Fields

    /** The unique identifier for the link. */
    private final UUID uuid;
    /** The URL of the link. */
    private final String url;
    /** The name of the link. */
    private final String name;
    /** The description of the link. */
    private final String description;
    // endregion

    @JsonCreator
    public Link(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("url") String url,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description
        ) {
            if (uuid == null) throw new IllegalArgumentException("uuid is required");
            this.uuid = uuid;
            this.url = url;
            this.name = name;
            this.description = description;
        }

        // region Getters
        @JsonProperty("uuid")
        public UUID uuid() { return uuid; }

        @JsonProperty("url")
        public String url() { return url; }

        @JsonProperty("name")
        public String name() { return name; }

        @JsonProperty("description")
        public String description() { return description; }

        // endregion
        
        @Override public boolean equals(Object o) {
            return (o instanceof Link) && uuid.equals(((Link) o).uuid);
        }

        @Override public int hashCode() {
            return uuid.hashCode();
        }
}