package com.serbekun.ss.repo.linksrepo;

import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.LinkRepository;

public interface LinkRepositoryReadInterface {
    Map<UUID, LinkRepository> getRepositoriesData();
}
