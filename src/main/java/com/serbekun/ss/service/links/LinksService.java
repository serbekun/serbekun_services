package com.serbekun.ss.service.links;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.repo.links.LinksRepo;
import com.serbekun.ss.repo.localtokens.LocalTokensRepo;

public class LinksService {

    // region Fields

    /** ObjectMapper for JSON serialization */
    private static final ObjectMapper mapper = new ObjectMapper();
    /** Repository for managing links */
    private final LinksRepo links;
    /** Repository for managing link-specific access tokens */
    private final LocalTokensRepo linkTokens;

    /** Logger for logging messages */
    private static final Logger log = 
        LoggerFactory.getLogger(LinksService.class);

    // endregion

    // region Constructors

    /**
     * Constructor for LinksService.
     * @param links Repository for managing links
     */
    public LinksService(LinksRepo links) {
        this(links, null);
    }

    /**
     * Constructor for LinksService with link token management.
     * @param links Repository for managing links
     * @param linkTokens Repository for managing link-specific access tokens
     */
    public LinksService(LinksRepo links, LocalTokensRepo linkTokens) {
        this.links = links;
        this.linkTokens = linkTokens;
    }

    // endregion


    // CRUD operations for links
     /**
     * Checks if a link exists in the repository.
     * @param uuid Link UUID
     * @return true if the link exists, false otherwise
     */
    public synchronized boolean existsLink(UUID uuid) {
        return links.existsLink(uuid);
    }

    /**
     * Adds a link to the repository.
     * @param link the {@link Link} object to be added
     */
    public synchronized void addLink(Link link) {
        links.addLink(link);
    }

    /**
     * Gets a link from the repository.
     * @param uuid Link UUID
     * @return the {@link Link} object, or null if not found
     */
    public synchronized Link getLink(UUID uuid) {
        return links.getLink(uuid);
    }

    /**
     * Returns all persisted links.
     * @return an immutable copy of the stored links map
     */
    public synchronized Map<UUID, Link> getAllLinks() {
        return links.getAllLinks();
    }


    /**
     * Returns all persisted links as a JSON string.
     * @return a JSON string representing all stored links, or null if serialization fails
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
     * Updates a link in the repository.
     * @param uuid the UUID of the link to update
     * @param newLink the new {@link Link} object to replace the existing one
     */
    public synchronized void updateLink(UUID uuid, Link newLink) {
        removeLink(uuid);
        addLink(newLink);
    }

    /**
     * Removes a link from the repository.
     * @param uuid the UUID of the link to remove
     * @return the removed {@link Link} object, or null if not found
     */
    public synchronized Link removeLink(UUID uuid) {
        return links.removeLink(uuid);
    }

    // endregion

    // region Business operations with link-specific access tokens 

    public synchronized String createLink(String url, String name, String description) {
        if (linkTokens == null) {
            throw new IllegalStateException("Link token manager not configured");
        }
        UUID uuid = UUID.randomUUID();
        Link link = new Link(uuid, url, name, description);
        links.addLink(link);
        return linkTokens.generateToken(uuid);
    }

    /**
     * Updates a link in the repository.
     * @param uuidStr the UUID of the link to update
     * @param token the access token for authentication
     * @param url the new URL for the link
     * @param name the new name for the link
     * @param description the new description for the link
     * @return the HTTP status code indicating the result of the operation
     */
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

    /**
     * Deletes a link from the repository.
     * @param uuidStr the UUID of the link to delete
     * @param token the access token for authentication
     * @return the HTTP status code indicating the result of the operation
     */
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

    // endregion
}
