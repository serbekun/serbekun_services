package com.serbekun.ss.repo.burnlink;

import java.util.Map;

import com.serbekun.ss.domain.models.BurnLink;

/**
 * Interface that provides {@link BurnLinkFileRepo} read-only access to the
 * in-memory store.
 */
public interface BurnLinkReadInterface {
    /**
     * @return All burn links keyed by their public id.
     */
    Map<String, BurnLink> getBurnLinkData();
}
