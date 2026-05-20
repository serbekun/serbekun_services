package com.serbekun.ss.service.http.handles.v0;

import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * implement request to /v0/api/json/{name}
 */
public class StaticV0Json {
    
    private final ResourcesService resourcesService;

    public StaticV0Json(ResourcesService resourcesService) {
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

        if (name == null || name.isEmpty()) {
            String path = ResourcesBasePath.BASE_JSON_PATH;
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

        String path = ResourcesBasePath.resolveJsonPath(name);
        String json = resourcesService.getTextData(path);

        return json;
    }
}
