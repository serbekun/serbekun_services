package com.serbekun.ss.repo.links;

import java.util.Map;
import java.util.UUID;

import com.serbekun.ss.domain.models.Link;

/**
 * Interface for provide to {@link com.serbekun.ss.repo.links.LinksFileRepo}
 * File Repository class only read access
 */
public interface LinksRepositoryReadInterface {
    /**
     * @return result of return {@link com.serbekun.ss.repo.links.LinksRepo#getAllLinks()}
     */
    Map<UUID, Link> getLinksData();
}
