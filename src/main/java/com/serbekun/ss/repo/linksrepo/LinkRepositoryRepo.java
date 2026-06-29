package com.serbekun.ss.repo.linksrepo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.Link;
import com.serbekun.ss.domain.models.LinkRepository;

public class LinkRepositoryRepo implements LinkRepositoryReadInterface {

    private final Map<UUID, LinkRepository> repos;

    public LinkRepositoryRepo(Map<UUID, LinkRepository> repos) {
        this.repos = new HashMap<>();
        if (repos != null) {
            this.repos.putAll(repos);
        }
    }

    public synchronized void addRepository(LinkRepository repo) {
        if (repo == null || repo.repositoryId() == null) return;
        repos.put(repo.repositoryId(), repo);
    }

    public synchronized LinkRepository getRepository(UUID repositoryId) {
        return (repositoryId == null) ? null : repos.get(repositoryId);
    }

    public synchronized LinkRepository findByToken(String token) {
        if (token == null || token.isBlank()) return null;
        for (LinkRepository repo : repos.values()) {
            if (token.equals(repo.token())) return repo;
        }
        return null;
    }

    public synchronized boolean existsRepository(UUID repositoryId) {
        return repositoryId != null && repos.containsKey(repositoryId);
    }

    public synchronized LinkRepository removeRepository(UUID repositoryId) {
        if (repositoryId == null) return null;
        return repos.remove(repositoryId);
    }

    public synchronized Link addLink(UUID repositoryId, Link link) {
        LinkRepository repo = repos.get(repositoryId);
        if (repo == null || link == null || link.uuid() == null) return null;
        repo.links().put(link.uuid(), link);
        return link;
    }

    public synchronized Link getLink(UUID repositoryId, UUID linkId) {
        LinkRepository repo = repos.get(repositoryId);
        if (repo == null) return null;
        return repo.links().get(linkId);
    }

    public synchronized Link removeLink(UUID repositoryId, UUID linkId) {
        LinkRepository repo = repos.get(repositoryId);
        if (repo == null) return null;
        return repo.links().remove(linkId);
    }

    @Override
    public synchronized Map<UUID, LinkRepository> getRepositoriesData() {
        return Map.copyOf(repos);
    }
}
