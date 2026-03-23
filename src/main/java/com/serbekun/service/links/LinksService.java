package com.serbekun.service.links;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.serbekun.core.Links;
import com.serbekun.core.Links.Link;
import com.serbekun.service.json.ProgramsJson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinksService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Links links;

    private static final Logger log = 
        LoggerFactory.getLogger(ProgramsJson.class);

    public LinksService(Links links) {
        this.links = links;
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
     * return all {@link Links#links}
     *
     * @return immutable copy of links map
     */
    public synchronized Map<UUID, Link> getAllLinks() {
        return links.getAllLinks();
    }

    /**
     * 
     * return json string of {@link Links#links}
     * 
     * @return json format String
     */
    public synchronized String getAllLinksAsJson() {
        try {
            return mapper.writeValueAsString(links.getAllLinks());
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
     * remove link from {@link Links#links}
     * 
     * @param uuid UUID of link
     */
    public synchronized Link removeLink(UUID uuid) {
        return links.removeLink(uuid);
    }
}
