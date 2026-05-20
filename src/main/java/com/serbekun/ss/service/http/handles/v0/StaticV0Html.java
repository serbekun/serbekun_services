package com.serbekun.ss.service.http.handles.v0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.resources.ResourcesBasePath;
import com.serbekun.ss.service.resource.ResourcesService;

import java.util.List;
import java.util.stream.Collectors;

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

        if (name == null || name.isEmpty()) {
            String path = ResourcesBasePath.BASE_HTML_PATH;
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

        String path = ResourcesBasePath.resolveHtmlPath(name);
        return resourcesService.getTextData(path);
    }


}
