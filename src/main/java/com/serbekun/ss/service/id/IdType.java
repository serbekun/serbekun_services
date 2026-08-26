package com.serbekun.ss.service.id;

import java.util.Locale;

/** What a generation request asks for. */
public enum IdType {

    /** A UUID, version 4 or 7. */
    UUID("uuid"),

    /** A ULID — time-ordered, Crockford base32. */
    ULID("ulid"),

    /** A random string drawn from an alphabet. */
    TOKEN("token"),

    /** Raw random bytes, written in a chosen format. */
    BYTES("bytes");

    private final String wireName;

    IdType(String wireName) {
        this.wireName = wireName;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /**
     * Resolves a caller-supplied type name.
     *
     * @param name the type as written by the caller
     * @return the matching type
     * @throws IllegalArgumentException when the name is blank or matches nothing
     */
    public static IdType fromWire(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("type is required — supported: " + supported());
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT);
        for (IdType type : values()) {
            if (type.wireName.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unsupported type '" + name + "' — supported: " + supported());
    }

    /** Comma separated list of the canonical names, for error messages. */
    public static String supported() {
        return "uuid, ulid, token, bytes";
    }
}
