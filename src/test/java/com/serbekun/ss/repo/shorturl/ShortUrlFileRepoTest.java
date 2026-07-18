package com.serbekun.ss.repo.shorturl;

import com.serbekun.ss.domain.models.ShortUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlFileRepoTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyMapWhenFileMissing() {
        ShortUrlFileRepo fileRepo = new ShortUrlFileRepo(tempDir.resolve("missing.json"));

        assertThat(fileRepo.load()).isEmpty();
    }

    @Test
    void loadReturnsEmptyMapOnCorruptFile() throws IOException {
        Path file = tempDir.resolve("short_url.json");
        Files.writeString(file, "{ not valid json ");
        ShortUrlFileRepo fileRepo = new ShortUrlFileRepo(file);

        assertThat(fileRepo.load()).isEmpty();
    }

    @Test
    void saveAndLoadRoundtrip() {
        Path file = tempDir.resolve("nested/short_url.json");
        ShortUrlRepo repo = new ShortUrlRepo(new HashMap<>());
        ShortUrl url = new ShortUrl("abc12345", "https://example.com", "token-1", "name", "desc", 123L);
        repo.addShortUrl(url);

        ShortUrlFileRepo fileRepo = new ShortUrlFileRepo(file);
        fileRepo.setShortUrlReadInterface(repo);
        fileRepo.save();

        Map<String, ShortUrl> loaded = new ShortUrlFileRepo(file).load();
        assertThat(loaded).hasSize(1);
        ShortUrl loadedUrl = loaded.get("abc12345");
        assertThat(loadedUrl.targetUrl()).isEqualTo("https://example.com");
        assertThat(loadedUrl.token()).isEqualTo("token-1");
        assertThat(loadedUrl.name()).isEqualTo("name");
        assertThat(loadedUrl.description()).isEqualTo("desc");
        assertThat(loadedUrl.createdTime()).isEqualTo(123L);
    }

    @Test
    void saveWithoutReadInterfaceDoesNotThrow() {
        ShortUrlFileRepo fileRepo = new ShortUrlFileRepo(tempDir.resolve("short_url.json"));

        fileRepo.save(); // logs an error but must not fail

        assertThat(Files.exists(tempDir.resolve("short_url.json"))).isFalse();
    }
}
