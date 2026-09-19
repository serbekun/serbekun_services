package com.serbekun.ss.domain.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A self-destructing link that serves a secret text exactly once.
 * <p>
 * The {@code id} is the public code carried in the link ({@code /b/{id}}).
 * The {@code token} is a separate secret issued at creation time and required
 * to delete the record early. {@code text} is the payload revealed by the first
 * visitor. {@code expiredTime} is an absolute epoch-millisecond deadline where
 * {@code 0} means "never"; it mirrors the convention used by
 * {@link UploadedFile}.
 * <p>
 * Access can be restricted by device type, browser and client IP. Empty lists
 * mean "no restriction"; a non-empty list is an allow list. The IP blacklist is
 * checked before the IP whitelist, so a blocked address always loses.
 */
public class BurnLink {

    private final String id;
    private final String text;
    private final String token;
    private final long createdTime;
    private final long expiredTime;
    private final List<String> devices;
    private final List<String> browsers;
    private final List<String> ipWhitelist;
    private final List<String> ipBlacklist;

    @JsonCreator
    public BurnLink(
            @JsonProperty("id") String id,
            @JsonProperty("text") String text,
            @JsonProperty("token") String token,
            @JsonProperty("created_time") long createdTime,
            @JsonProperty("expired_time") long expiredTime,
            @JsonProperty("devices") List<String> devices,
            @JsonProperty("browsers") List<String> browsers,
            @JsonProperty("ip_whitelist") List<String> ipWhitelist,
            @JsonProperty("ip_blacklist") List<String> ipBlacklist) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        this.id = id;
        this.text = text;
        this.token = token;
        this.createdTime = createdTime;
        this.expiredTime = expiredTime;
        this.devices = devices == null ? List.of() : List.copyOf(devices);
        this.browsers = browsers == null ? List.of() : List.copyOf(browsers);
        this.ipWhitelist = ipWhitelist == null ? List.of() : List.copyOf(ipWhitelist);
        this.ipBlacklist = ipBlacklist == null ? List.of() : List.copyOf(ipBlacklist);
    }

    @JsonProperty("id")
    public String id() { return id; }

    @JsonProperty("text")
    public String text() { return text; }

    @JsonProperty("token")
    public String token() { return token; }

    @JsonProperty("created_time")
    public long createdTime() { return createdTime; }

    @JsonProperty("expired_time")
    public long expiredTime() { return expiredTime; }

    @JsonProperty("devices")
    public List<String> devices() { return devices; }

    @JsonProperty("browsers")
    public List<String> browsers() { return browsers; }

    @JsonProperty("ip_whitelist")
    public List<String> ipWhitelist() { return ipWhitelist; }

    @JsonProperty("ip_blacklist")
    public List<String> ipBlacklist() { return ipBlacklist; }

    @Override public boolean equals(Object o) {
        return (o instanceof BurnLink) && id.equals(((BurnLink) o).id);
    }

    @Override public int hashCode() {
        return id.hashCode();
    }
}
