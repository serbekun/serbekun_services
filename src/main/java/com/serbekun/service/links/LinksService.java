package com.serbekun.service.links;

import java.util.Map;
import java.util.UUID;

import com.serbekun.core.Links;
import com.serbekun.core.Links.Link;

public class LinksService {

    private final Links links;

    public LinksService(Links links) {
        this.links = links;
    }

        /**
     * 
     * Add link to catalog
     * 
     * @param link {@link Link} object which will be added to catalog
     */
    public synchronized void addLink(Link link) {
        links.addLink(link);
    }

    /**
     * 
     * get {@link Link} object
     * 
     * @return {@link Link} object
     */
    public synchronized Link getLink(UUID uuid) {
        return links.getLink(uuid);
    }

    /**
     * return all {@link Links#links}
     *
     * @return immutable copy of links map
     */
    public synchronized Map<UUID, Link> getAllLinks() {
        return links.getAllLinks();
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
        return links.removeLink(uuid);
    }
}
