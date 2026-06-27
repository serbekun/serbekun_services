package com.serbekun.ss.repo.shorturl;

import java.util.Map;

import com.serbekun.ss.domain.models.ShortUrl;

/**
 * Interface for provide to {@link com.serbekun.ss.repo.shorturl.ShortUrlFileRepo}
 * File Repository class only read access
 */
public interface ShortUrlReadInterface {
    /**
     * @return All short url records keyed by their short id.
     */
    Map<String, ShortUrl> getShortUrlData();
}
