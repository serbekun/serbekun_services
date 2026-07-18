package com.serbekun.ss.service.resource;

import com.serbekun.ss.resources.ResourceCache;
import com.serbekun.ss.resources.ResourceLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ResourcesService against the real classpath resources
 * (src/main/resources is on the test classpath).
 */
class ResourcesServiceStaticsTest {

    private ResourcesService service;

    @BeforeEach
    void setUp() {
        ResourceLoader loader = new ResourceLoader();
        service = new ResourcesService(loader, new ResourceCache(loader));
    }

    @Test
    void detectMimeTypeKnowsCommonExtensions() {
        assertThat(service.detectMimeType("a.html")).isEqualTo("text/html");
        assertThat(service.detectMimeType("a.css")).isEqualTo("text/css");
        assertThat(service.detectMimeType("a.js")).isEqualTo("application/javascript");
        assertThat(service.detectMimeType("a.json")).isEqualTo("application/json");
        assertThat(service.detectMimeType("a.JPG")).isEqualTo("image/jpeg");
        assertThat(service.detectMimeType("a.svg")).isEqualTo("image/svg+xml");
        assertThat(service.detectMimeType("a.pdf")).isEqualTo("application/pdf");
    }

    @Test
    void detectMimeTypeFallsBackToOctetStream() {
        assertThat(service.detectMimeType("a.unknown")).isEqualTo("application/octet-stream");
        assertThat(service.detectMimeType("no-extension")).isEqualTo("application/octet-stream");
        assertThat(service.detectMimeType("trailing-dot.")).isEqualTo("application/octet-stream");
    }

    @Test
    void getHtmlReturnsIndexPage() {
        String html = service.getHtml("index.html");

        assertThat(html).isNotNull();
        assertThat(html.toLowerCase()).contains("<html");
    }

    @Test
    void getHtmlWithEmptyNameReturnsJsonListing() {
        String listing = service.getHtml("");

        assertThat(listing).isNotNull();
        assertThat(listing).startsWith("[").contains("index.html");
    }

    @Test
    void getTextDataReturnsNullForMissingResource() {
        assertThat(service.getTextData("html/definitely-missing.html")).isNull();
    }

    @Test
    void getImageReturnsNullForEmptyName() {
        assertThat(service.getImage("")).isNull();
        assertThat(service.getImage(null)).isNull();
    }

    @Test
    void listResourcesFindsHtmlFiles() {
        assertThat(service.listResources("html/")).contains("html/index.html");
    }
}
