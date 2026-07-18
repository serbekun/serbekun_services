package com.serbekun.ss.repo.linksrepo;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LinkRepositoryRepoTest {

    private LinkRepositoryRepo repo;

    @BeforeEach
    void setUp() {
        repo = new LinkRepositoryRepo(new HashMap<>());
    }

    private LinkRepository newRepository(String token) {
        return new LinkRepository(UUID.randomUUID(), token, "name", "2026-01-01T00:00:00Z", Map.of());
    }

    @Test
    void addAndGetRepository() {
        LinkRepository repository = newRepository("t1");
        repo.addRepository(repository);

        assertThat(repo.getRepository(repository.repositoryId())).isEqualTo(repository);
        assertThat(repo.existsRepository(repository.repositoryId())).isTrue();
    }

    @Test
    void nullSafety() {
        repo.addRepository(null);

        assertThat(repo.getRepository(null)).isNull();
        assertThat(repo.existsRepository(null)).isFalse();
        assertThat(repo.removeRepository(null)).isNull();
        assertThat(repo.findByToken(null)).isNull();
        assertThat(repo.findByToken("  ")).isNull();
        assertThat(repo.getRepositoriesData()).isEmpty();
    }

    @Test
    void findByTokenReturnsMatchingRepository() {
        LinkRepository repository = newRepository("secret-token");
        repo.addRepository(repository);

        assertThat(repo.findByToken("secret-token")).isEqualTo(repository);
        assertThat(repo.findByToken("other")).isNull();
    }

    @Test
    void removeRepositoryReturnsRemovedValue() {
        LinkRepository repository = newRepository("t1");
        repo.addRepository(repository);

        assertThat(repo.removeRepository(repository.repositoryId())).isEqualTo(repository);
        assertThat(repo.existsRepository(repository.repositoryId())).isFalse();
    }

    @Test
    void addGetRemoveLink() {
        LinkRepository repository = newRepository("t1");
        repo.addRepository(repository);
        Link link = new Link(UUID.randomUUID(), "https://example.com", "n", "d");

        assertThat(repo.addLink(repository.repositoryId(), link)).isEqualTo(link);
        assertThat(repo.getLink(repository.repositoryId(), link.uuid())).isEqualTo(link);
        assertThat(repo.removeLink(repository.repositoryId(), link.uuid())).isEqualTo(link);
        assertThat(repo.getLink(repository.repositoryId(), link.uuid())).isNull();
    }

    @Test
    void linkOperationsOnUnknownRepositoryReturnNull() {
        Link link = new Link(UUID.randomUUID(), "https://example.com", null, null);
        UUID unknownRepo = UUID.randomUUID();

        assertThat(repo.addLink(unknownRepo, link)).isNull();
        assertThat(repo.getLink(unknownRepo, link.uuid())).isNull();
        assertThat(repo.removeLink(unknownRepo, link.uuid())).isNull();
    }

    @Test
    void constructorCopiesInitialMap() {
        Map<UUID, LinkRepository> initial = new HashMap<>();
        LinkRepository repository = newRepository("t1");
        initial.put(repository.repositoryId(), repository);
        LinkRepositoryRepo fromInitial = new LinkRepositoryRepo(initial);

        initial.clear();

        assertThat(fromInitial.existsRepository(repository.repositoryId())).isTrue();
    }
}
