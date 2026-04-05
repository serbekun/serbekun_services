package com.serbekun.ss.service.http.handles.v0;

import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

public class V0Page {
    
    private final ResourcesService resourcesService;

    public V0Page(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    /**
     * 
     * Run endpoint business logic
     * 
     * @param name
     * @return
     */
    public String run(String name) {

        String path = ResourcesBasePath.resolveHtmlPath(name);
        return resourcesService.getTextData(path);
    }


}
