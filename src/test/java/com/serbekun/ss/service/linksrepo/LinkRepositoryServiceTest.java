package com.serbekun.ss.service.linksrepo;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinkRepository;
import com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LinkRepositoryServiceTest {

    private LinkRepositoryRepo repo;
    private LinkRepositoryService service;

    @BeforeEach
    void setUp() {
        repo = new LinkRepositoryRepo(new HashMap<>());
        service = new LinkRepositoryService(repo);
    }

    // region createRepository

    @Test
    void createRepositoryStoresRepositoryWithTokenAndName() {
        LinkRepository created = service.createRepository("my repo");

        assertThat(created.name()).isEqualTo("my repo");
        assertThat(created.token()).isNotBlank();
        assertThat(created.createdAt()).isNotBlank();
        assertThat(created.links()).isEmpty();
        assertThat(repo.getRepository(created.repositoryId())).isEqualTo(created);
    }

    @Test
    void createRepositoryUsesIdAsDefaultNameWhenBlank() {
        LinkRepository noName = service.createRepository(null);
        LinkRepository blankName = service.createRepository("   ");

        assertThat(noName.name()).isEqualTo(noName.repositoryId().toString());
        assertThat(blankName.name()).isEqualTo(blankName.repositoryId().toString());
    }

    // endregion

    // region getRepository / removeRepository

    @Test
    void getRepositoryRequiresValidToken() {
        LinkRepository created = service.createRepository("repo");

        assertThat(service.getRepository(created.repositoryId(), created.token())).isEqualTo(created);
        assertThat(service.getRepository(created.repositoryId(), "wrong-token")).isNull();
        assertThat(service.getRepository(UUID.randomUUID(), created.token())).isNull();
    }

    @Test
    void removeRepositoryRequiresValidToken() {
        LinkRepository created = service.createRepository("repo");

        assertThat(service.removeRepository(created.repositoryId(), "wrong-token")).isEqualTo(404);
        assertThat(service.removeRepository(UUID.randomUUID(), created.token())).isEqualTo(404);
        assertThat(repo.existsRepository(created.repositoryId())).isTrue();

        assertThat(service.removeRepository(created.repositoryId(), created.token())).isEqualTo(200);
        assertThat(repo.existsRepository(created.repositoryId())).isFalse();
    }

    // endregion

    // region addLink

    @Test
    void addLinkStoresLinkInRepository() {
        LinkRepository created = service.createRepository("repo");

        Link link = service.addLink(created.repositoryId(), created.token(),
                "https://example.com", "example", "an example");

        assertThat(link).isNotNull();
        assertThat(link.url()).isEqualTo("https://example.com");
        assertThat(repo.getLink(created.repositoryId(), link.uuid())).isEqualTo(link);
    }

    @Test
    void addLinkRejectsInvalidTokenOrUnknownRepository() {
        LinkRepository created = service.createRepository("repo");

        assertThat(service.addLink(created.repositoryId(), "wrong-token", "https://a", null, null)).isNull();
        assertThat(service.addLink(UUID.randomUUID(), created.token(), "https://a", null, null)).isNull();
    }

    @Test
    void addLinkRejectsBlankUrl() {
        LinkRepository created = service.createRepository("repo");

        assertThat(service.addLink(created.repositoryId(), created.token(), null, null, null)).isNull();
        assertThat(service.addLink(created.repositoryId(), created.token(), "  ", null, null)).isNull();
    }

    // endregion

    // region updateLink

    @Test
    void updateLinkReplacesLinkData() {
        LinkRepository created = service.createRepository("repo");
        Link link = service.addLink(created.repositoryId(), created.token(), "https://old", "old", "old desc");

        int status = service.updateLink(created.repositoryId(), created.token(), link.uuid(),
                "https://new", "new", "new desc");

        assertThat(status).isEqualTo(200);
        Link updated = repo.getLink(created.repositoryId(), link.uuid());
        assertThat(updated.url()).isEqualTo("https://new");
        assertThat(updated.name()).isEqualTo("new");
        assertThat(updated.description()).isEqualTo("new desc");
    }

    @Test
    void updateLinkReturns400ForBlankUrl() {
        LinkRepository created = service.createRepository("repo");
        Link link = service.addLink(created.repositoryId(), created.token(), "https://old", null, null);

        assertThat(service.updateLink(created.repositoryId(), created.token(), link.uuid(), "  ", null, null))
            .isEqualTo(400);
        // original link untouched
        assertThat(repo.getLink(created.repositoryId(), link.uuid()).url()).isEqualTo("https://old");
    }

    @Test
    void updateLinkReturns404ForUnknownLinkOrInvalidToken() {
        LinkRepository created = service.createRepository("repo");
        Link link = service.addLink(created.repositoryId(), created.token(), "https://old", null, null);

        assertThat(service.updateLink(created.repositoryId(), created.token(), UUID.randomUUID(), "https://x", null, null))
            .isEqualTo(404);
        assertThat(service.updateLink(created.repositoryId(), "wrong-token", link.uuid(), "https://x", null, null))
            .isEqualTo(404);
        assertThat(service.updateLink(UUID.randomUUID(), created.token(), link.uuid(), "https://x", null, null))
            .isEqualTo(404);
    }

    // endregion

    // region deleteLink

    @Test
    void deleteLinkRemovesLink() {
        LinkRepository created = service.createRepository("repo");
        Link link = service.addLink(created.repositoryId(), created.token(), "https://a", null, null);

        assertThat(service.deleteLink(created.repositoryId(), created.token(), link.uuid())).isEqualTo(200);
        assertThat(repo.getLink(created.repositoryId(), link.uuid())).isNull();
    }

    @Test
    void deleteLinkReturns404ForUnknownLinkOrInvalidToken() {
        LinkRepository created = service.createRepository("repo");
        Link link = service.addLink(created.repositoryId(), created.token(), "https://a", null, null);

        assertThat(service.deleteLink(created.repositoryId(), created.token(), UUID.randomUUID())).isEqualTo(404);
        assertThat(service.deleteLink(created.repositoryId(), "wrong-token", link.uuid())).isEqualTo(404);
        assertThat(service.deleteLink(UUID.randomUUID(), created.token(), link.uuid())).isEqualTo(404);
    }

    // endregion
}
