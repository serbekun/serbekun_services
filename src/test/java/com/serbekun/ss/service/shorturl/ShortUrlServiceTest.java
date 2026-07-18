package com.serbekun.ss.service.shorturl;

import com.serbekun.ss.domain.models.ShortUrl;
import com.serbekun.ss.repo.shorturl.ShortUrlRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShortUrlServiceTest {

    private ShortUrlRepo repo;
    private ShortUrlService service;

    @BeforeEach
    void setUp() {
        repo = new ShortUrlRepo(new HashMap<>());
        service = new ShortUrlService(repo);
    }

    @Test
    void createShortUrlGeneratesBase62IdAndToken() {
        ShortUrl shortUrl = service.createShortUrl("https://example.com", "name", "desc");

        assertThat(shortUrl.id()).matches("[A-Za-z0-9]{8}");
        assertThat(shortUrl.token()).isNotBlank();
        assertThat(shortUrl.targetUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.name()).isEqualTo("name");
        assertThat(shortUrl.description()).isEqualTo("desc");
        assertThat(shortUrl.createdTime()).isGreaterThan(0);
        assertThat(repo.getShortUrl(shortUrl.id())).isEqualTo(shortUrl);
    }

    @Test
    void createShortUrlTrimsTargetUrl() {
        ShortUrl shortUrl = service.createShortUrl("  https://example.com  ", null, null);

        assertThat(shortUrl.targetUrl()).isEqualTo("https://example.com");
    }

    @Test
    void createShortUrlRejectsNullOrBlankTarget() {
        assertThatThrownBy(() -> service.createShortUrl(null, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.createShortUrl("   ", null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getShortUrlReturnsNullForUnknownId() {
        assertThat(service.getShortUrl("unknown1")).isNull();
    }

    @Test
    void existsReflectsRepositoryState() {
        ShortUrl shortUrl = service.createShortUrl("https://example.com", null, null);

        assertThat(service.exists(shortUrl.id())).isTrue();
        assertThat(service.exists("missing1")).isFalse();
    }

    @Test
    void deleteShortUrlReturns404ForUnknownId() {
        assertThat(service.deleteShortUrl("missing1", "any-token")).isEqualTo(404);
    }

    @Test
    void deleteShortUrlReturns403ForWrongOrMissingToken() {
        ShortUrl shortUrl = service.createShortUrl("https://example.com", null, null);

        assertThat(service.deleteShortUrl(shortUrl.id(), "wrong-token")).isEqualTo(403);
        assertThat(service.deleteShortUrl(shortUrl.id(), null)).isEqualTo(403);
        assertThat(service.exists(shortUrl.id())).isTrue();
    }

    @Test
    void deleteShortUrlWithValidTokenRemovesRecord() {
        ShortUrl shortUrl = service.createShortUrl("https://example.com", null, null);

        assertThat(service.deleteShortUrl(shortUrl.id(), shortUrl.token())).isEqualTo(204);
        assertThat(service.exists(shortUrl.id())).isFalse();
    }
}
