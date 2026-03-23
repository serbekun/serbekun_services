package com.serbekun.core;

import java.util.UUID;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 
 * Core class that manage links catalog on server 
 */
public class Links {

    public static final class Link {
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

    private final Map<UUID, Link> linksByUuid;

    public Links(Map<UUID, Link> linksByUID) {
        this.linksByUuid = linksByUID;
    }

    /**
     * 
     * @param uuid Link UUID
     * @return true link exist false uuid don't exist
     */
    public synchronized boolean existsLink(UUID uuid) {
        return linksByUuid.containsKey(uuid);
    }

    /**
     * 
     * Add link to catalog
     * 
     * @param link {@link Link} object which will be added to catalog
     */
    public synchronized void addLink(Link link) {
        if (link == null || link.uuid == null) {
            return;
        }
        linksByUuid.put(link.uuid, link);
    }

    /**
     * 
     * get {@link Link} object
     * 
     * @return {@link Link} object
     */
    public synchronized Link getLink(UUID uuid) {
        return (uuid == null) ? null : linksByUuid.get(uuid);
    }

    /**
     * return all {@link Links#links}
     *
     * @return immutable copy of links map
     */
    public synchronized Map<UUID, Link> getAllLinks() {
        return Map.copyOf(linksByUuid);
    }

    /**
     * 
     * helper method for update Link
     * 
     * @param uuid UUID of {@link Link} which will be updated
     * @param newLink new {@link Link} object
     */
    public synchronized void updateLink(UUID uuid, Link newLink) {
        removeLink(uuid);
        addLink(newLink);
    }

    /**
     * 
     * remove link from {@link Links#links}
     * 
     * @param uuid UUID of link
     */
    public synchronized Link removeLink(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return linksByUuid.remove(uuid);
    }
}
