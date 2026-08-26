package com.serbekun.ss.service.id;

import java.util.Locale;

/**
 * The UUID versions this service issues.
 * <p>
 * {@link #V4} is 122 random bits and nothing else — the safe default when an id
 * must reveal nothing. {@link #V7} puts a millisecond timestamp in the leading
 * 48 bits, so ids sort by creation time; that makes them far kinder to a
 * database index, at the cost of telling anyone who reads one when it was made.
 */
public enum UuidVersion {

    V4("v4"),
    V7("v7");

    private final String wireName;

    UuidVersion(String wireName) {
        this.wireName = wireName;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /**
     * Resolves a caller-supplied version.
     *
     * @param name {@code v4} / {@code 4} / {@code v7} / {@code 7}; blank means {@link #V4}
     * @return the matching version
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static UuidVersion fromWire(String name) {
        if (name == null || name.isBlank()) {
            return V4;
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("v")) {
            normalized = "v" + normalized;
        }

        for (UuidVersion version : values()) {
            if (version.wireName.equals(normalized)) {
                return version;
            }
        }
        throw new IllegalArgumentException("unsupported version '" + name + "' — supported: v4, v7");
    }
}
