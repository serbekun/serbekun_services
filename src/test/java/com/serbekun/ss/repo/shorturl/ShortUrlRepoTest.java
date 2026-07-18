package com.serbekun.ss.repo.shorturl;

import com.serbekun.ss.domain.models.ShortUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlRepoTest {

    private ShortUrlRepo repo;

    @BeforeEach
    void setUp() {
        repo = new ShortUrlRepo(new HashMap<>());
    }

    private ShortUrl shortUrl(String id) {
        return new ShortUrl(id, "https://example.com", "token", null, null, 1L);
    }

    @Test
    void addAndGetShortUrl() {
        ShortUrl url = shortUrl("abc12345");
        repo.addShortUrl(url);

        assertThat(repo.getShortUrl("abc12345")).isEqualTo(url);
        assertThat(repo.existsShortUrl("abc12345")).isTrue();
    }

    @Test
    void addIgnoresNull() {
        repo.addShortUrl(null);

        assertThat(repo.getShortUrlData()).isEmpty();
    }

    @Test
    void getAndExistsAreNullSafe() {
        assertThat(repo.getShortUrl(null)).isNull();
        assertThat(repo.existsShortUrl(null)).isFalse();
        assertThat(repo.removeShortUrl(null)).isNull();
    }

    @Test
    void removeReturnsRemovedValue() {
        ShortUrl url = shortUrl("abc12345");
        repo.addShortUrl(url);

        assertThat(repo.removeShortUrl("abc12345")).isEqualTo(url);
        assertThat(repo.existsShortUrl("abc12345")).isFalse();
        assertThat(repo.removeShortUrl("abc12345")).isNull();
    }

    @Test
    void getShortUrlDataReturnsSnapshotCopy() {
        repo.addShortUrl(shortUrl("abc12345"));
        var snapshot = repo.getShortUrlData();
        repo.addShortUrl(shortUrl("def67890"));

        assertThat(snapshot).hasSize(1);
        assertThat(repo.getShortUrlData()).hasSize(2);
    }
}
