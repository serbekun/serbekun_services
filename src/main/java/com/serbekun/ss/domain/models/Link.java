package com.serbekun.ss.domain.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Link {

    private final UUID uuid;
    private final String url;
    private final String name;
    private final String description;

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

        @JsonProperty("uuid")
        public UUID uuid() { return uuid; }

        @JsonProperty("url")
        public String url() { return url; }

        @JsonProperty("name")
        public String name() { return name; }

        @JsonProperty("description")
        public String description() { return description; }

        @Override public boolean equals(Object o) {
            return (o instanceof Link) && uuid.equals(((Link) o).uuid);
        }

        @Override public int hashCode() {
            return uuid.hashCode();
        }
}