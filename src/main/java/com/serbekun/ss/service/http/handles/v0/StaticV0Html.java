package com.serbekun.ss.service.http.handles.v0;

import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0Html {
    
    private final ResourcesService resourcesService;

    public StaticV0Html(ResourcesService resourcesService) {
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
