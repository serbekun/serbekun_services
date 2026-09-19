package com.serbekun.ss.repo.burnlink;

import java.util.Map;

import com.serbekun.ss.domain.models.BurnLink;

/**
 * In-memory repository for self-destructing links, keyed by their public id.
 */
public class BurnLinkRepo implements BurnLinkReadInterface {

    private final Map<String, BurnLink> burnLinks;

    public BurnLinkRepo(Map<String, BurnLink> burnLinks) {
        this.burnLinks = burnLinks;
    }

    public synchronized boolean existsBurnLink(String id) {
        return id != null && burnLinks.containsKey(id);
    }

    public synchronized void addBurnLink(BurnLink burnLink) {
        if (burnLink == null || burnLink.id() == null) {
            return;
        }
        burnLinks.put(burnLink.id(), burnLink);
    }

    public synchronized BurnLink getBurnLink(String id) {
        return (id == null) ? null : burnLinks.get(id);
    }

    public synchronized BurnLink removeBurnLink(String id) {
        if (id == null) {
            return null;
        }
        return burnLinks.remove(id);
    }

    @Override
    public synchronized Map<String, BurnLink> getBurnLinkData() {
        return Map.copyOf(burnLinks);
    }
}
