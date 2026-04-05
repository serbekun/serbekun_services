package com.serbekun.ss.service.http.handles.v0;

import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0Images {
    
    private final ResourcesService resourcesService;

    public StaticV0Images(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    /**
     * 
     * run endpoint business logic
     * 
     * @param name Image name
     * @return bytes of image
     */
    public byte[] run(String name) {
        
        String path = ResourcesBasePath.resolveImagePath(name);
        return resourcesService.getBinaryData(path);

    }

}
