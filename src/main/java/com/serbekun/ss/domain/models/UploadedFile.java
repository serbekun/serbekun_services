package com.serbekun.ss.domain.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadedFile {

    private final UUID uuid;
    private final String name;
    private final String token;
    private final long expiredTime;

    @JsonCreator
    public UploadedFile(
            @JsonProperty("uuid") UUID uuid,
            @JsonProperty("name") String name,
            @JsonProperty("token") String token,
            @JsonProperty("expired_time") long expiredTime
        ) {
            if (uuid == null) throw new IllegalArgumentException("uuid is required");
            this.uuid = uuid;
            this.name = name;
            this.token = token;
            this.expiredTime = expiredTime;
        }

        @JsonProperty("uuid")
        public UUID uuid() { return uuid; }

        @JsonProperty("token")
        public String token() { return token; }

        @JsonProperty("name")
        public String name() { return name; }

        @JsonProperty("expired_time")
        public long expiredTime() {
            return expiredTime;
        }

        @Override public boolean equals(Object o) {
            return (o instanceof UploadedFile) && uuid.equals(((UploadedFile) o).uuid);
        }

        @Override public int hashCode() {
            return uuid.hashCode();
        }
}