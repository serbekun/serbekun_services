package com.serbekun.ss.repo.links;

import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.Link;

public class LinksRepo implements LinksRepositoryReadInterface {

    private final Map<UUID, Link> linksByUuid;
    
    public LinksRepo(Map<UUID, Link> linksByUID) {
        this.linksByUuid = linksByUID;
    }

    /**
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
        if (link == null || link.uuid() == null) {
            return;
        }
        linksByUuid.put(link.uuid(), link);
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
     * Return all {@link LinksRepo#links}
     *
     * @return Immutable copy of links map.
     */
    public synchronized Map<UUID, Link> getAllLinks() {
        return Map.copyOf(linksByUuid);
    }

    @Override
    public synchronized Map<UUID, Link> getLinksData() {
        return getAllLinks();
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
     * remove link from {@link LinksRepo#links}
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
