package com.serbekun.ss.service.burnlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class UserAgentClassifierTest {

    @Test
    void detectsDeviceTypes() {
        assertThat(UserAgentClassifier.device(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1"))
            .isEqualTo("iphone");
        assertThat(UserAgentClassifier.device(
                "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1"))
            .isEqualTo("ipad");
        assertThat(UserAgentClassifier.device(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"))
            .isEqualTo("android");
        assertThat(UserAgentClassifier.device(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/121.0"))
            .isEqualTo("windows");
        assertThat(UserAgentClassifier.device(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15"))
            .isEqualTo("mac");
        assertThat(UserAgentClassifier.device(
                "Mozilla/5.0 (X11; Linux x86_64) Firefox/121.0"))
            .isEqualTo("linux");
        assertThat(UserAgentClassifier.device(null)).isEqualTo("other");
        assertThat(UserAgentClassifier.device("something odd")).isEqualTo("other");
    }

    @Test
    void detectsBrowsersBeforeTheirImpersonators() {
        // Edge claims Chrome, Chrome claims Safari.
        assertThat(UserAgentClassifier.browser(
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0 Safari/537.36 Edg/120.0"))
            .isEqualTo("edge");
        assertThat(UserAgentClassifier.browser(
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0 Safari/537.36 OPR/106.0"))
            .isEqualTo("opera");
        assertThat(UserAgentClassifier.browser(
                "Mozilla/5.0 (Windows NT 10.0; rv:121.0) Gecko/20100101 Firefox/121.0"))
            .isEqualTo("firefox");
        assertThat(UserAgentClassifier.browser(
                "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0 Safari/537.36"))
            .isEqualTo("chrome");
        assertThat(UserAgentClassifier.browser(
                "Mozilla/5.0 (Macintosh) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15"))
            .isEqualTo("safari");
        assertThat(UserAgentClassifier.browser(null)).isEqualTo("other");
    }

    @Test
    void allowedTreatsEmptyListAsNoRestriction() {
        assertThat(UserAgentClassifier.allowed("iphone", List.of())).isTrue();
        assertThat(UserAgentClassifier.allowed("iphone", null)).isTrue();
        assertThat(UserAgentClassifier.allowed("iphone", List.of("iphone"))).isTrue();
        assertThat(UserAgentClassifier.allowed("android", List.of("iphone"))).isFalse();
    }
}
