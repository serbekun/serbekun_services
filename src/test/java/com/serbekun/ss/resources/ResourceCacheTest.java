package com.serbekun.ss.resources;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceCacheTest {

    @Test
    void binaryResourceIsLoadedOnceAndCached() {
        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.loadBinary("images/a.jpg")).thenReturn(new byte[] {1, 2, 3});
        ResourceCache cache = new ResourceCache(loader);

        assertThat(cache.getBinary("images/a.jpg")).containsExactly(1, 2, 3);
        assertThat(cache.getBinary("images/a.jpg")).containsExactly(1, 2, 3);

        verify(loader, times(1)).loadBinary("images/a.jpg");
        assertThat(cache.isBinaryCached("images/a.jpg")).isTrue();
    }

    @Test
    void missingBinaryResourceIsNotCached() {
        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.loadBinary("images/missing.jpg")).thenReturn(null);
        ResourceCache cache = new ResourceCache(loader);

        assertThat(cache.getBinary("images/missing.jpg")).isNull();
        assertThat(cache.getBinary("images/missing.jpg")).isNull();

        verify(loader, times(2)).loadBinary("images/missing.jpg");
        assertThat(cache.isBinaryCached("images/missing.jpg")).isFalse();
    }

    @Test
    void textResourceIsLoadedOnceAndCached() {
        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.loadText("html/a.html", StandardCharsets.UTF_8)).thenReturn("<html></html>");
        ResourceCache cache = new ResourceCache(loader);

        assertThat(cache.getText("html/a.html", StandardCharsets.UTF_8)).isEqualTo("<html></html>");
        assertThat(cache.getText("html/a.html", StandardCharsets.UTF_8)).isEqualTo("<html></html>");

        verify(loader, times(1)).loadText("html/a.html", StandardCharsets.UTF_8);
        assertThat(cache.isTextCached("html/a.html")).isTrue();
    }

    @Test
    void clearForcesReload() {
        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.loadBinary("images/a.jpg")).thenReturn(new byte[] {1});
        ResourceCache cache = new ResourceCache(loader);

        cache.getBinary("images/a.jpg");
        cache.clear();
        cache.getBinary("images/a.jpg");

        verify(loader, times(2)).loadBinary("images/a.jpg");
    }

    @Test
    void existsDelegatesToLoader() {
        ResourceLoader loader = mock(ResourceLoader.class);
        when(loader.exists("html/a.html")).thenReturn(true);
        ResourceCache cache = new ResourceCache(loader);

        assertThat(cache.exists("html/a.html")).isTrue();
        verify(loader).exists("html/a.html");
    }
}
