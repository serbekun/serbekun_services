package com.serbekun.ss.service.shorturl;

import java.security.SecureRandom;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.ShortUrl;
import com.serbekun.ss.repo.shorturl.ShortUrlRepo;

/**
 * Service for managing shortened URLs: create, resolve and delete.
 * <p>
 * The short id is a random base62 code that acts as the only key required
 * to resolve the redirect or delete the record.
 */
public class ShortUrlService {

    /** Logger for the short url service. */
    private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);

    /** Characters used to build short ids (base62). */
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** Length of generated short ids. */
    private static final int ID_LENGTH = 8;

    /** The repository for managing short url records. */
    private final ShortUrlRepo repo;

    /** Source of randomness for short id generation. */
    private final SecureRandom random = new SecureRandom();

    public ShortUrlService(ShortUrlRepo repo) {
        this.repo = repo;
    }

    /**
     * Create a new short url for the given target.
     *
     * @param targetUrl   the URL the short link should redirect to (required)
     * @param name        optional human-readable name
     * @param description optional description
     * @return the created {@link ShortUrl}
     * @throws IllegalArgumentException if {@code targetUrl} is null or blank
     */
    public synchronized ShortUrl createShortUrl(String targetUrl, String name, String description) {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new IllegalArgumentException("targetUrl must not be null or blank");
        }

        String id = generateUniqueId();
        String token = UUID.randomUUID().toString();
        ShortUrl shortUrl = new ShortUrl(
                id,
                targetUrl.trim(),
                token,
                name,
                description,
                System.currentTimeMillis());

        repo.addShortUrl(shortUrl);
        log.info("Created short url id={} -> {}", id, targetUrl);
        return shortUrl;
    }

    /** Resolve a short url by its id. Returns null if not found. */
    public synchronized ShortUrl getShortUrl(String id) {
        return repo.getShortUrl(id);
    }

    /** Check whether a short url with the given id exists. */
    public synchronized boolean exists(String id) {
        return repo.existsShortUrl(id);
    }

    /**
     * Delete a short url. Requires the token issued at creation time.
     *
     * @return HTTP-like status: 204 on success, 403 if token mismatch, 404 if not found.
     */
    public synchronized int deleteShortUrl(String id, String token) {
        ShortUrl existing = repo.getShortUrl(id);
        if (existing == null) {
            return 404;
        }
        if (token == null || !token.equals(existing.token())) {
            return 403;
        }
        repo.removeShortUrl(id);
        log.info("Deleted short url id={}", id);
        return 204;
    }

    /** Generate a short id that is not already used. */
    private String generateUniqueId() {
        String id;
        do {
            id = randomId();
        } while (repo.existsShortUrl(id));
        return id;
    }

    /** Generate a random base62 id of {@link #ID_LENGTH} characters. */
    private String randomId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
