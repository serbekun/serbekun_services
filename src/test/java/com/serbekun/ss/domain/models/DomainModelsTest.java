package com.serbekun.ss.domain.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // region ShortUrl

    @Test
    void shortUrlRequiresIdAndTargetUrl() {
        assertThatThrownBy(() -> new ShortUrl(null, "https://a", "t", null, null, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShortUrl(" ", "https://a", "t", null, null, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShortUrl("id123456", null, "t", null, null, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShortUrl("id123456", " ", "t", null, null, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shortUrlEqualityIsById() {
        ShortUrl a = new ShortUrl("same-id", "https://a", "t1", null, null, 1);
        ShortUrl b = new ShortUrl("same-id", "https://b", "t2", null, null, 2);
        ShortUrl c = new ShortUrl("other-id", "https://a", "t1", null, null, 1);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shortUrlJacksonRoundtripUsesSnakeCase() throws Exception {
        ShortUrl original = new ShortUrl("abc12345", "https://example.com", "tok", "n", "d", 42L);

        String json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"target_url\"").contains("\"created_time\"");

        ShortUrl restored = mapper.readValue(json, ShortUrl.class);
        assertThat(restored.id()).isEqualTo("abc12345");
        assertThat(restored.targetUrl()).isEqualTo("https://example.com");
        assertThat(restored.token()).isEqualTo("tok");
        assertThat(restored.createdTime()).isEqualTo(42L);
    }

    // endregion

    // region UploadedFile

    @Test
    void uploadedFileRequiresUuid() {
        assertThatThrownBy(() -> new UploadedFile(null, "a", "t", 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadedFileJacksonRoundtripUsesSnakeCase() throws Exception {
        UUID uuid = UUID.randomUUID();
        UploadedFile original = new UploadedFile(uuid, "file.txt", "tok", 99L);

        String json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"expired_time\"");

        UploadedFile restored = mapper.readValue(json, UploadedFile.class);
        assertThat(restored.uuid()).isEqualTo(uuid);
        assertThat(restored.name()).isEqualTo("file.txt");
        assertThat(restored.expiredTime()).isEqualTo(99L);
    }

    // endregion

    // region Link

    @Test
    void linkRequiresUuid() {
        assertThatThrownBy(() -> new Link(null, "https://a", null, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void linkEqualityIsByUuid() {
        UUID uuid = UUID.randomUUID();
        assertThat(new Link(uuid, "https://a", null, null))
            .isEqualTo(new Link(uuid, "https://b", "x", "y"));
    }

    // endregion

    // region LinkRepository

    @Test
    void linkRepositoryRequiresIdAndToken() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new LinkRepository(null, "t", "n", "c", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LinkRepository(id, null, "n", "c", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LinkRepository(id, "  ", "n", "c", Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void linkRepositoryCopiesInitialLinksMap() {
        UUID linkId = UUID.randomUUID();
        Map<UUID, Link> initial = new HashMap<>();
        initial.put(linkId, new Link(linkId, "https://a", null, null));

        LinkRepository repository = new LinkRepository(UUID.randomUUID(), "t", "n", "c", initial);
        initial.clear();

        assertThat(repository.links()).hasSize(1);
    }

    @Test
    void linkRepositoryAcceptsNullLinksMap() {
        LinkRepository repository = new LinkRepository(UUID.randomUUID(), "t", "n", "c", null);

        assertThat(repository.links()).isEmpty();
    }

    // endregion
}
