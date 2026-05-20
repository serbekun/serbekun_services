package com.serbekun.ss.service.http.handles.v0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     *
     * Returns JSON list of available image files.
     *
     * @return json string with file names
     */
    public String list() {
        String path = ResourcesBasePath.BASE_IMAGES_PATH;
        List<String> filesList = resourcesService.listResources(path).stream()
            .map(file -> file.startsWith(path) ? file.substring(path.length()) : file)
            .collect(Collectors.toList());

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(filesList);
        } catch (Exception e) {
            return null;
        }
    }

}
