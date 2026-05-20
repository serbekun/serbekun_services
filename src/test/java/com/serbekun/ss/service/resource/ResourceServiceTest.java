package com.serbekun.ss.service.resource;

import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.resources.ResourcesBasePath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceServiceTest {

    @Test
    void AvailableFileListTest() {
        ResourceLoader loader = mock(ResourceLoader.class);
        ResourceCache cache = mock(ResourceCache.class);
        ResourcesService resourcesService = new ResourcesService(loader, cache);

        List<String> expectedFiles = List.of(
            "html/index.html",
            "html/links.html"
        );
        when(cache.listResources(ResourcesBasePath.BASE_HTML_PATH)).thenReturn(expectedFiles);

        List<String> actualFiles = resourcesService.listResources(ResourcesBasePath.BASE_HTML_PATH);

        assertThat(actualFiles).containsExactlyElementsOf(expectedFiles);
        verify(cache).listResources(ResourcesBasePath.BASE_HTML_PATH);
    }
}
