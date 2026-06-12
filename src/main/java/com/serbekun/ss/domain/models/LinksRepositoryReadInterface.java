package com.serbekun.ss.domain.models;

import java.util.Map;
import java.util.UUID;

/**
 * Interface for provide to {@link com.serbekun.ss.repository.LinksFileRepositoryImpl}
 * File Repository class only read access
 */
public interface LinksRepositoryReadInterface {
    /**
     * @return result of return {@link com.serbekun.ss.domain.models.LinksRepository#getAllLinks()}
     */
    Map<UUID, Link> getLinksData();
}
