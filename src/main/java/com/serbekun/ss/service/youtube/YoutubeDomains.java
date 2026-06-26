package com.serbekun.ss.service.youtube;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.serbekun.ss.resources.ResourceLoader;
import com.serbekun.ss.resources.ResourcesBasePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and validates YouTube domain rules from domain/youtube.txt resource.
 *
 * Domain file format:
 * - Plain domain (e.g. "youtube.com"): matches the domain and all subdomains.
 * - "full:" prefix (e.g. "full:yt3.googleusercontent.com"): matches only the exact hostname.
 * - Annotations after " @" are ignored (e.g. "ggpht.cn @cn" → "ggpht.cn").
 */
public class YoutubeDomains {

    private static final Logger log = LoggerFactory.getLogger(YoutubeDomains.class);

    private final Set<String> suffixDomains;
    private final Set<String> exactDomains;

    /**
     * Creates a new YoutubeDomains instance by loading and parsing the domain file.
     *
     * @param resourceLoader the resource loader to use
     * @throws IllegalStateException if the domain resource cannot be loaded or parsed
     */
    public YoutubeDomains(ResourceLoader resourceLoader) {
        String path = ResourcesBasePath.resolveDomainPath("youtube.txt");
        String raw = resourceLoader.loadText(path, StandardCharsets.UTF_8);
        if (raw == null || raw.isBlank()) {
            log.error("Failed to load YouTube domains from {}", path);
            throw new IllegalStateException("YouTube domain data not found at " + path);
        }

        Set<String> suffix = new HashSet<>();
        Set<String> exact = new HashSet<>();

        for (String line : raw.lines().collect(Collectors.toList())) {
            String domain = parseLine(line);
            if (domain == null) {
                continue;
            }
            if (domain.startsWith("full:")) {
                exact.add(domain.substring(5));
            } else {
                suffix.add(domain);
            }
        }

        this.suffixDomains = Collections.unmodifiableSet(suffix);
        this.exactDomains = Collections.unmodifiableSet(exact);

        log.info("Loaded {} YouTube suffix domains and {} exact domains",
                suffixDomains.size(), exactDomains.size());
    }

    /**
     * Creates a YoutubeDomains instance with pre-parsed domain sets (for testing).
     *
     * @param suffixDomains domains that allow subdomain matching
     * @param exactDomains domains that require exact match
     */
    public YoutubeDomains(Set<String> suffixDomains, Set<String> exactDomains) {
        this.suffixDomains = Collections.unmodifiableSet(new HashSet<>(suffixDomains));
        this.exactDomains = Collections.unmodifiableSet(new HashSet<>(exactDomains));
    }

    /**
     * Parses a single line from the domain file.
     * Strips annotations (text after " @") and trims whitespace.
     *
     * @param line raw line from the domain file
     * @return parsed domain or null if the line is empty/comment
     */
    private static String parseLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Strip annotation: "ggpht.cn @cn" → "ggpht.cn"
        int annotationIdx = trimmed.indexOf(" @");
        if (annotationIdx >= 0) {
            trimmed = trimmed.substring(0, annotationIdx).trim();
        }
        return trimmed;
    }

    /**
     * Checks whether a URL string has an allowed YouTube domain.
     *
     * @param url the URL to check
     * @return true if the URL's host matches an allowed domain
     * @throws IllegalArgumentException if the URL cannot be parsed
     */
    public boolean isAllowed(String url) {
        String host = extractHost(url);
        if (host == null) {
            throw new IllegalArgumentException("Could not extract host from URL: " + url);
        }
        return isHostAllowed(host);
    }

    /**
     * Checks whether a hostname is in the allowed domains list.
     *
     * @param host the hostname to check
     * @return true if allowed
     */
    public boolean isHostAllowed(String host) {
        // Check exact matches first
        if (exactDomains.contains(host)) {
            return true;
        }
        // Check suffix matches: host equals domain OR ends with ".domain"
        for (String domain : suffixDomains) {
            if (host.equals(domain) || host.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the hostname from a URL string.
     *
     * @param url the URL to parse
     * @return the hostname, or null if parsing fails
     */
    static String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                // Try with a scheme if missing
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    uri = new URI("https://" + url);
                    host = uri.getHost();
                }
            }
            return host;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns the set of suffix-match domains (for inspection/testing).
     */
    public Set<String> getSuffixDomains() {
        return suffixDomains;
    }

    /**
     * Returns the set of exact-match domains (for inspection/testing).
     */
    public Set<String> getExactDomains() {
        return exactDomains;
    }
}
