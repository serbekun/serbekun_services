package com.serbekun.ss.repo.shorturl;

import java.util.Map;

import com.serbekun.ss.domain.models.ShortUrl;

/**
 * In-memory repository for shortened URLs, keyed by their short id.
 */
public class ShortUrlRepo implements ShortUrlReadInterface {

    private final Map<String, ShortUrl> shortUrls;

    public ShortUrlRepo(Map<String, ShortUrl> shortUrls) {
        this.shortUrls = shortUrls;
    }

    public synchronized boolean existsShortUrl(String id) {
        return id != null && shortUrls.containsKey(id);
    }

    public synchronized void addShortUrl(ShortUrl shortUrl) {
        if (shortUrl == null || shortUrl.id() == null) {
            return;
        }
        shortUrls.put(shortUrl.id(), shortUrl);
    }

    public synchronized ShortUrl getShortUrl(String id) {
        return (id == null) ? null : shortUrls.get(id);
    }

    public synchronized ShortUrl removeShortUrl(String id) {
        if (id == null) {
            return null;
        }
        return shortUrls.remove(id);
    }

    @Override
    public synchronized Map<String, ShortUrl> getShortUrlData() {
        return Map.copyOf(shortUrls);
    }
}
