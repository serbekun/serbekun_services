package com.serbekun.service.http.handles.v0;

import com.serbekun.resources.ResourcesBasePath;
import com.serbekun.service.resource.ResourcesService;

/**
 * implement request to /v0/api/json/{name}
 */
public class V0ApiJson {
    
    private final ResourcesService resourcesService;

    public V0ApiJson(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    /**
     * 
     * Endpoint business logic start method
     * 
     * @param name json file name WITHOUT ''.json'
     * @return json string
     */
    public String run(String name) {

        String path = ResourcesBasePath.resolveJsonPath(name);
        String json = resourcesService.getTextData(path);

        return json;
    }
}
