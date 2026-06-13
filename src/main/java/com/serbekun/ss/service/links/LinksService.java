package com.serbekun.ss.service.links;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinksRepo;
import com.serbekun.ss.domain.models.LocalTokens;

public class LinksService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final LinksRepo links;
    private final LocalTokens linkTokens;

    private static final Logger log = 
        LoggerFactory.getLogger(LinksService.class);

    public LinksService(LinksRepo links) {
        this(links, null);
    }

    public LinksService(LinksRepo links, LocalTokens linkTokens) {
        this.links = links;
        this.linkTokens = linkTokens;
    }

     /**
     * 
     * @param uuid Link UUID
     * @return true link exist false uuid don't exist
     */
    public synchronized boolean existsLink(UUID uuid) {
        return links.existsLink(uuid);
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
     * return all {@link LinksRepo#links}
     *
     * @return immutable copy of links map
     */
    public synchronized Map<UUID, Link> getAllLinks() {
        return links.getAllLinks();
    }

    /**
     * 
     * return json string of {@link LinksRepo#links}
     * 
     * @return json format String
     */
    public synchronized String getAllLinksAsJson() {
        try {
            return mapper.writeValueAsString(links.getAllLinks().values());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize links to JSON", e);
            return null;
        }
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
        return links.removeLink(uuid);
    }

    // === Business operations with link-specific access tokens ===

    public synchronized String createLink(String url, String name, String description) {
        if (linkTokens == null) {
            throw new IllegalStateException("Link token manager not configured");
        }
        UUID uuid = UUID.randomUUID();
        Link link = new Link(uuid, url, name, description);
        links.addLink(link);
        return linkTokens.generateToken(uuid);
    }

    public synchronized int updateLink(String uuidStr, String token, String url, String name, String description) {
        if (linkTokens == null) {
            return 500;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (Exception e) {
            return 400;
        }
        if (!links.existsLink(uuid)) {
            return 404;
        }
        if (!linkTokens.hasAccess(token, uuid)) {
            return 403;
        }
        Link newLink = new Link(uuid, url, name, description);
        links.updateLink(uuid, newLink);
        return 200;
    }

    public synchronized int deleteLink(String uuidStr, String token) {
        if (linkTokens == null) {
            return 500;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (Exception e) {
            return 400;
        }
        if (!links.existsLink(uuid)) {
            return 404;
        }
        if (!linkTokens.hasAccess(token, uuid)) {
            return 403;
        }
        linkTokens.removeToken(token);
        links.removeLink(uuid);
        return 200;
    }
}
