package com.serbekun.ss.repo.linksrepo;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LinkRepositoryFileRepoTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsEmptyMapWhenFileMissing() {
        LinkRepositoryFileRepo fileRepo = new LinkRepositoryFileRepo(tempDir.resolve("missing.json"));

        assertThat(fileRepo.load()).isEmpty();
    }

    @Test
    void loadReturnsEmptyMapOnCorruptFile() throws IOException {
        Path file = tempDir.resolve("links.json");
        Files.writeString(file, "not json at all");
        LinkRepositoryFileRepo fileRepo = new LinkRepositoryFileRepo(file);

        assertThat(fileRepo.load()).isEmpty();
    }

    @Test
    void saveAndLoadRoundtripWithLinks() {
        Path file = tempDir.resolve("nested/links.json");
        UUID repositoryId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        Link link = new Link(linkId, "https://example.com", "example", "desc");
        LinkRepository repository = new LinkRepository(
                repositoryId, "token-1", "repo name", "2026-07-19T00:00:00Z", Map.of(linkId, link));

        LinkRepositoryRepo repo = new LinkRepositoryRepo(new HashMap<>());
        repo.addRepository(repository);

        LinkRepositoryFileRepo fileRepo = new LinkRepositoryFileRepo(file);
        fileRepo.setReadInterface(repo);
        fileRepo.save();

        Map<UUID, LinkRepository> loaded = new LinkRepositoryFileRepo(file).load();
        assertThat(loaded).containsKey(repositoryId);
        LinkRepository loadedRepo = loaded.get(repositoryId);
        assertThat(loadedRepo.token()).isEqualTo("token-1");
        assertThat(loadedRepo.name()).isEqualTo("repo name");
        assertThat(loadedRepo.links()).hasSize(1);
        assertThat(loadedRepo.links().get(linkId).url()).isEqualTo("https://example.com");
    }

    @Test
    void saveWithoutReadInterfaceDoesNotThrow() {
        LinkRepositoryFileRepo fileRepo = new LinkRepositoryFileRepo(tempDir.resolve("links.json"));

        fileRepo.save();

        assertThat(Files.exists(tempDir.resolve("links.json"))).isFalse();
    }
}
