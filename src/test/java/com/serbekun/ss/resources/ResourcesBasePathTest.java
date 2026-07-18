package com.serbekun.ss.resources;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourcesBasePathTest {

    @Test
    void resolvesNamesAgainstBasePaths() {
        assertThat(ResourcesBasePath.resolveHtmlPath("index.html")).isEqualTo("html/index.html");
        assertThat(ResourcesBasePath.resolveCssPath("shared.css")).isEqualTo("css/shared.css");
        assertThat(ResourcesBasePath.resolveJsPath("index.js")).isEqualTo("js/index.js");
        assertThat(ResourcesBasePath.resolveSvgPath("icon.svg")).isEqualTo("svg/icon.svg");
        assertThat(ResourcesBasePath.resolveImagePath("a.jpg")).isEqualTo("images/a.jpg");
        assertThat(ResourcesBasePath.resolveJsonPath("a.json")).isEqualTo("json/a.json");
        assertThat(ResourcesBasePath.resolvePdfPath("a.pdf")).isEqualTo("pdf/a.pdf");
        assertThat(ResourcesBasePath.resolveDomainPath("youtube.txt")).isEqualTo("domain/youtube.txt");
    }

    @Test
    void nullNameResolvesToBarePath() {
        assertThat(ResourcesBasePath.resolve("html/", null)).isEqualTo("html/");
    }

    @Test
    void appendsSlashToBaseWithoutTrailingSlash() {
        assertThat(ResourcesBasePath.resolve("html", "a.html")).isEqualTo("html/a.html");
    }

    @Test
    void rejectsPathTraversalSequences() {
        assertThatThrownBy(() -> ResourcesBasePath.resolveHtmlPath("../secret"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourcesBasePath.resolveHtmlPath("a/b.html"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourcesBasePath.resolveHtmlPath("a\\b.html"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourcesBasePath.resolveHtmlPath("%2e%2e%2fsecret"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
