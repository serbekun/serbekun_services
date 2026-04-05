package com.serbekun.ss.service.http.handles.v0;

import java.util.UUID;

import com.serbekun.ss.core.LocalTokens;
import com.serbekun.ss.core.Links.Link;
import com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksDeleteRequest;
import com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPostRequest;
import com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPutRequest;
import com.serbekun.ss.service.links.LinksService;

/**
 * Implement requests to /v0/api/catalog/links
 */
public class ApiV0CatalogsLinks {
    
    private final LinksService linksService;
    private final LocalTokens tokens;

    public ApiV0CatalogsLinks(LinksService linksService, LocalTokens tokens) {
        this.linksService = linksService;
        this.tokens = tokens;
    }

    /**
     * 
     * Implement GET request
     * 
     * @return json String
     */
    public String get() {
        return linksService.getAllLinksAsJson();
    }

    /**
     * 
     * Implement POST request
     * 
     * @param v0LinksPostRequest {@link com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPostRequest} object
     * @return token for access to resource
     */
    public String post(V0LinksPostRequest v0LinksPostRequest) {   

        UUID resourceUUID = UUID.randomUUID();

        Link link = new Link(resourceUUID,
        v0LinksPostRequest.url,
        v0LinksPostRequest.name,
        v0LinksPostRequest.description);

        linksService.addLink(link);

        // token for access to resource
        String token = tokens.generateToken(resourceUUID);

        return token;
    }

    /**
     * 
     * Implement PUT request
     * 
     * @param v0LinksPutRequest {@link com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksPutRequest} object
     * @return 403 Unauthorized, 404 link not fond, 200 link successfully updated
     */
    public int put(V0LinksPutRequest v0LinksPutRequest) {
        
        UUID uuid = UUID.fromString(v0LinksPutRequest.uuid);
        
        if (!linksService.existsLink(uuid)) {
            return 404;
        }
        
        // check token
        if (!tokens.hasAccess(v0LinksPutRequest.token, uuid)) {
            return 403;
        }


        Link link = new Link(uuid,
        v0LinksPutRequest.url,
        v0LinksPutRequest.name,
        v0LinksPutRequest.description);

        linksService.updateLink(uuid, link);
        return 200;
    }

    /**
     * 
     * Implement delete request
     * 
     * @param v0LinksDeleteRequest {@link com.serbekun.ss.service.http.handles.v0.dto.links.V0LinksDeleteRequest} object
     * @return 403 Unauthorized, 404 link not fond, 200 link successfully deleted
     */
    public int delete(V0LinksDeleteRequest v0LinksDeleteRequest) {
        
        UUID uuid = UUID.fromString(v0LinksDeleteRequest.uuid);
        
        if (!linksService.existsLink(uuid)) {
            return 404;
        }
        
        if (!tokens.hasAccess(v0LinksDeleteRequest.token, uuid)) {
            return 403;
        }

        linksService.removeLink(uuid);
        return 200;
    }
}
