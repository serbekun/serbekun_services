package com.serbekun.ss.service.burnlink;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.ss.domain.models.BurnLink;
import com.serbekun.ss.repo.burnlink.BurnLinkRepo;
import com.serbekun.ss.service.security.Secrets;

/**
 * Business logic for self-destructing links: create, resolve (without burning),
 * reveal (burn exactly once) and delete.
 * <p>
 * The link only burns on {@link #reveal}, never on {@link #resolve}. That keeps
 * messaging-app preview fetchers — which only issue a GET — from consuming the
 * secret before a human opens it. Reveal is synchronized, so of two concurrent
 * visitors exactly one receives the text.
 */
public class BurnLinkService {

    private static final Logger log = LoggerFactory.getLogger(BurnLinkService.class);

    /** Characters used to build public ids (base62). */
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /** Length of generated public ids. */
    private static final int ID_LENGTH = 10;

    /** Maximum accepted secret size, in characters. */
    public static final int MAX_TEXT_LENGTH = 100_000;

    private final BurnLinkRepo repo;
    private final SecureRandom random = new SecureRandom();

    public BurnLinkService(BurnLinkRepo repo) {
        this.repo = repo;
    }

    /**
     * Creates a burn link. {@code ttlSeconds} follows the uploaded-files
     * convention: {@code 0} or less means it never expires.
     *
     * @throws IllegalArgumentException on blank/oversized text, negative ttl,
     *         unknown device/browser values or invalid IP entries
     */
    public synchronized BurnLink create(String text, String name, long ttlSeconds,
            List<String> devices, List<String> browsers,
            List<String> ipWhitelist, List<String> ipBlacklist) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be null or blank");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("text must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        if (ttlSeconds < 0) {
            throw new IllegalArgumentException("ttl must not be negative");
        }

        List<String> normalizedDevices = normalizeDevices(devices);
        List<String> normalizedBrowsers = normalizeBrowsers(browsers);
        List<String> normalizedIpWhitelist = normalizeIps(ipWhitelist, "ipWhitelist");
        List<String> normalizedIpBlacklist = normalizeIps(ipBlacklist, "ipBlacklist");

        long expiredTime = ttlSeconds > 0
                ? System.currentTimeMillis() + ttlSeconds * 1000L
                : 0;

        String id = generateUniqueId();
        String token = UUID.randomUUID().toString();
        BurnLink link = new BurnLink(
                id,
                text,
                token,
                System.currentTimeMillis(),
                expiredTime,
                normalizedDevices,
                normalizedBrowsers,
                normalizedIpWhitelist,
                normalizedIpBlacklist);

        repo.addBurnLink(link);
        log.info("Created burn link id={} ttl={}s devices={} browsers={} ips={}/{}",
                id, ttlSeconds, normalizedDevices, normalizedBrowsers,
                normalizedIpWhitelist.size(), normalizedIpBlacklist.size());
        return link;
    }

    /**
     * Resolves a link for display without burning it. Returns null when the
     * link is missing, expired or the caller is not on the allow list — the
     * three cases are deliberately indistinguishable to the caller.
     */
    public synchronized BurnLink resolve(String id, String userAgent, String clientIp) {
        BurnLink link = liveLink(id);
        if (link == null || !canAccess(link, userAgent, clientIp)) {
            return null;
        }
        return link;
    }

    /**
     * Burns a link and returns its secret, or null when it is missing, expired,
     * already burned or the caller is not allowed. Only a successful reveal
     * removes the record, so a blocked visitor cannot destroy the link for the
     * intended recipient.
     */
    public synchronized String reveal(String id, String userAgent, String clientIp) {
        BurnLink link = liveLink(id);
        if (link == null || !canAccess(link, userAgent, clientIp)) {
            return null;
        }
        repo.removeBurnLink(id);
        log.info("Burned link id={}", id);
        return link.text();
    }

    /**
     * @return HTTP-like status: 204 on success, 403 on token mismatch, 404 if
     *         not found or already expired.
     */
    public synchronized int delete(String id, String token) {
        BurnLink existing = liveLink(id);
        if (existing == null) {
            return 404;
        }
        if (!Secrets.constantTimeEquals(token, existing.token())) {
            return 403;
        }
        repo.removeBurnLink(id);
        log.info("Deleted burn link id={}", id);
        return 204;
    }

    /**
     * Removes every link whose deadline has passed. Links with
     * {@code expiredTime == 0} never expire.
     *
     * @return the number of removed links
     */
    public synchronized int deleteExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (BurnLink link : repo.getBurnLinkData().values()) {
            if (link.expiredTime() > 0 && link.expiredTime() < now) {
                repo.removeBurnLink(link.id());
                removed++;
                log.info("Removed expired burn link id={}", link.id());
            }
        }
        if (removed > 0) {
            log.info("Cleaned up {} expired burn link(s)", removed);
        }
        return removed;
    }

    private BurnLink liveLink(String id) {
        BurnLink link = repo.getBurnLink(id);
        if (link == null) {
            return null;
        }
        if (link.expiredTime() > 0 && System.currentTimeMillis() >= link.expiredTime()) {
            repo.removeBurnLink(id);
            log.info("Removed expired burn link id={}", id);
            return null;
        }
        return link;
    }

    private boolean canAccess(BurnLink link, String userAgent, String clientIp) {
        if (!UserAgentClassifier.allowed(UserAgentClassifier.device(userAgent), link.devices())) {
            return false;
        }
        if (!UserAgentClassifier.allowed(UserAgentClassifier.browser(userAgent), link.browsers())) {
            return false;
        }
        return IpAccessList.isAllowed(clientIp, link.ipWhitelist(), link.ipBlacklist());
    }

    private List<String> normalizeDevices(List<String> devices) {
        List<String> result = normalizeList(devices);
        for (String device : result) {
            if (!UserAgentClassifier.DEVICES.contains(device)) {
                throw new IllegalArgumentException("unknown device: " + device
                        + " (supported: " + String.join(", ", UserAgentClassifier.DEVICES) + ")");
            }
        }
        return result;
    }

    private List<String> normalizeBrowsers(List<String> browsers) {
        List<String> result = normalizeList(browsers);
        for (String browser : result) {
            if (!UserAgentClassifier.BROWSERS.contains(browser)) {
                throw new IllegalArgumentException("unknown browser: " + browser
                        + " (supported: " + String.join(", ", UserAgentClassifier.BROWSERS) + ")");
            }
        }
        return result;
    }

    private List<String> normalizeIps(List<String> ips, String field) {
        List<String> result = new ArrayList<>(normalizeList(ips));
        for (String ip : result) {
            if (!IpAccessList.isValidEntry(ip)) {
                throw new IllegalArgumentException(field + " contains an invalid IP or CIDR: " + ip);
            }
        }
        return List.copyOf(result);
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> distinct = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                distinct.add(value.trim().toLowerCase());
            }
        }
        return List.copyOf(distinct);
    }

    private String generateUniqueId() {
        String id;
        do {
            id = randomId();
        } while (repo.existsBurnLink(id));
        return id;
    }

    private String randomId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
