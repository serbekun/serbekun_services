package com.serbekun.ss.service.hash;

import java.util.Locale;

/**
 * The hash algorithms the API accepts.
 * <p>
 * {@link #fromWire(String)} is deliberately forgiving about spelling — callers
 * write {@code sha256}, {@code SHA-256} or {@code sha_256} and mean the same
 * thing — while {@link #wireName()} gives the one canonical spelling that every
 * response echoes back.
 */
public enum HashAlgorithm {

    SHA256("sha256", false),
    SHA512("sha512", false),
    BLAKE3("blake3", false),
    HMAC_SHA256("hmac-sha256", true);

    private final String wireName;
    private final boolean keyed;

    HashAlgorithm(String wireName, boolean keyed) {
        this.wireName = wireName;
        this.keyed = keyed;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /** Whether the algorithm needs a secret key — true only for HMAC. */
    public boolean isKeyed() {
        return keyed;
    }

    /**
     * Resolves a caller-supplied algorithm name.
     *
     * @param name the name as written by the caller, in any common spelling
     * @return the matching algorithm
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static HashAlgorithm fromWire(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("algorithm is required — supported: " + supported());
        }

        String normalized = name.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        for (HashAlgorithm algorithm : values()) {
            if (algorithm.wireName.replace("-", "").equals(normalized)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException("unsupported algorithm '" + name + "' — supported: " + supported());
    }

    /** Comma separated list of the canonical names, for error messages. */
    public static String supported() {
        StringBuilder names = new StringBuilder();
        for (HashAlgorithm algorithm : values()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(algorithm.wireName);
        }
        return names.toString();
    }
}
