package com.serbekun.ss.service.linksrepo;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinkRepository;
import com.serbekun.ss.repo.linksrepo.LinkRepositoryRepo;

/**
 * Service class for managing link repositories and their associated links. Provides methods to create, retrieve, update, and delete link repositories and links.
 * This class ensures thread-safe operations on the underlying repository.
 */
public class LinkRepositoryService {

    private final LinkRepositoryRepo repo;

    public LinkRepositoryService(LinkRepositoryRepo repo) {
        this.repo = repo;
    }

    /**
     * Creates a new link repository with the specified name. If the name is null or blank,
     *  a default name based on the generated repositoryId will be used.
     * @param name the name of the new link repository; if null or blank, a default name will be generated
     * @return the newly created LinkRepository instance
     */
    public synchronized LinkRepository createRepository(String name) {
        UUID repositoryId = UUID.randomUUID();
        String token = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();
        if (name == null || name.isBlank()) {
            name = repositoryId.toString();
        }
        LinkRepository repository = new LinkRepository(repositoryId, token, name, createdAt, Map.of());
        repo.addRepository(repository);
        return repository;
    }

    /**
     * Removes a link repository with the specified UUID and token.
     * @param uuid the UUID of the link repository to remove
     * @param token the token for authentication
     * @return 200 if the repository was successfully removed, 404 if not found or the token is invalid
     */
    public synchronized int removeRepository(UUID uuid, String token) {
        LinkRepository linkRepository = repo.getRepository(uuid);
        if (linkRepository == null || !linkRepository.token().equals(token)) {
            return 404; // Not found or token is invalid (do not reveal which)
        }

        repo.removeRepository(uuid);
        return 200; // Success
    }

    /**
     * Retrieves a link repository with the specified UUID and token.
     * @param repositoryId the UUID of the link repository to retrieve
     * @param token the token for authentication
     * @return the LinkRepository instance if found and authenticated, null otherwise
     */
    public synchronized LinkRepository getRepository(UUID repositoryId, String token) {
        LinkRepository r = repo.getRepository(repositoryId);
        if (r == null) return null;
        if (!r.token().equals(token)) return null;
        return r;
    }

    /**
     * Adds a new link to the specified link repository.
     * @param repositoryId the UUID of the link repository to which the link will be added
     * @param token the token for authentication
     * @param url the URL of the link
     * @param name the name of the link
     * @param description the description of the link
     * @return the newly created Link instance if successful, null otherwise
     */
    public synchronized Link addLink(UUID repositoryId, String token, String url, String name, String description) {
        LinkRepository r = repo.getRepository(repositoryId);
        if (r == null) return null;
        if (!r.token().equals(token)) return null;
        if (url == null || url.isBlank()) return null;
        UUID linkId = UUID.randomUUID();
        Link link = new Link(linkId, url, name, description);
        repo.addLink(repositoryId, link);
        return link;
    }

    /**
     * Updates an existing link in the specified link repository.
     * @param repositoryId the UUID of the link repository containing the link to update
     * @param token the token for authentication
     * @param linkId the UUID of the link to update
     * @param url the updated URL of the link
     * @param name the updated name of the link
     * @param description the updated description of the link
     * @return 200 if successful, 400 if the URL is invalid, 404 if not found or the token is invalid
     */
    public synchronized int updateLink(UUID repositoryId, String token, UUID linkId, String url, String name, String description) {
        LinkRepository r = repo.getRepository(repositoryId);
        if (r == null || !r.token().equals(token)) return 404;
        Link existing = repo.getLink(repositoryId, linkId);
        if (existing == null) return 404;
        if (url == null || url.isBlank()) return 400;
        repo.removeLink(repositoryId, linkId);
        Link newLink = new Link(linkId, url, name, description);
        repo.addLink(repositoryId, newLink);
        return 200;
    }

    /**
     * Deletes a link from the specified link repository.
     * @param repositoryId the UUID of the link repository from which the link will be deleted
     * @param token the token for authentication
     * @param linkId the UUID of the link to delete
     * @return 200 if successful, 404 if not found or the token is invalid
     */
    public synchronized int deleteLink(UUID repositoryId, String token, UUID linkId) {
        LinkRepository r = repo.getRepository(repositoryId);
        if (r == null || !r.token().equals(token)) return 404;
        Link removed = repo.removeLink(repositoryId, linkId);
        if (removed == null) return 404;
        return 200;
    }
}
