package com.serbekun.ss.service.id;

import java.util.Locale;
import java.util.UUID;

/** How a UUID is written out. The bits are the same in every one of them. */
public enum UuidFormat {

    /** {@code 8-4-4-4-12}, lowercase — what "a UUID" means unless something says otherwise. */
    CANONICAL("canonical"),

    /** The same 32 hex digits with the hyphens dropped. */
    COMPACT("compact"),

    /** Canonical, uppercase — the spelling Microsoft tooling tends to emit. */
    UPPER("upper"),

    /** {@code urn:uuid:…}, the RFC 4122 URN form. */
    URN("urn");

    private final String wireName;

    UuidFormat(String wireName) {
        this.wireName = wireName;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /**
     * Writes a UUID in this format.
     *
     * @param uuid the value to write
     * @return the formatted text
     */
    public String format(UUID uuid) {
        String canonical = uuid.toString();
        return switch (this) {
            case CANONICAL -> canonical;
            case COMPACT -> canonical.replace("-", "");
            case UPPER -> canonical.toUpperCase(Locale.ROOT);
            case URN -> "urn:uuid:" + canonical;
        };
    }

    /**
     * Resolves a caller-supplied format name.
     *
     * @param name the format as written by the caller; blank means {@link #CANONICAL}
     * @return the matching format
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static UuidFormat fromWire(String name) {
        if (name == null || name.isBlank()) {
            return CANONICAL;
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return switch (normalized) {
            case "canonical", "default", "hyphenated", "dashed" -> CANONICAL;
            case "compact", "hex", "nodashes", "nohyphens", "plain" -> COMPACT;
            case "upper", "uppercase" -> UPPER;
            case "urn" -> URN;
            default -> throw new IllegalArgumentException(
                "unsupported format '" + name + "' — supported: canonical, compact, upper, urn");
        };
    }
}
