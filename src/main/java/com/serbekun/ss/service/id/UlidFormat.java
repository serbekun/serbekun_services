package com.serbekun.ss.service.id;

import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/** How a ULID is written out. All four carry the same 128 bits. */
public enum UlidFormat {

    /** 26 characters of Crockford base32, uppercase — the ULID spec's own form. */
    CANONICAL("canonical"),

    /** The same 26 characters in lowercase, for a url that reads better. */
    LOWER("lower"),

    /**
     * The same bits written as a canonical UUID — a ULID fits a {@code uuid}
     * column exactly. It is not an RFC 4122 UUID though: the nibbles a UUID
     * spends on its version and variant carry ULID randomness here.
     */
    UUID("uuid"),

    /** The same bits as 32 hex digits. */
    HEX("hex");

    private final String wireName;

    UlidFormat(String wireName) {
        this.wireName = wireName;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /**
     * Writes a ULID in this format.
     *
     * @param value the 16 bytes to write
     * @return the formatted text
     */
    public String format(byte[] value) {
        return switch (this) {
            case CANONICAL -> Ulid.encode(value);
            case LOWER -> Ulid.encode(value).toLowerCase(Locale.ROOT);
            case UUID -> asUuid(value).toString();
            case HEX -> HexFormat.of().formatHex(value);
        };
    }

    private static java.util.UUID asUuid(byte[] value) {
        long high = 0;
        long low = 0;
        for (int i = 0; i < 8; i++) {
            high = (high << 8) | (value[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            low = (low << 8) | (value[i] & 0xFF);
        }
        return new java.util.UUID(high, low);
    }

    /**
     * Resolves a caller-supplied format name.
     *
     * @param name the format as written by the caller; blank means {@link #CANONICAL}
     * @return the matching format
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static UlidFormat fromWire(String name) {
        if (name == null || name.isBlank()) {
            return CANONICAL;
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return switch (normalized) {
            case "canonical", "default", "upper", "uppercase", "base32", "crockford" -> CANONICAL;
            case "lower", "lowercase" -> LOWER;
            case "uuid", "guid" -> UUID;
            case "hex", "base16" -> HEX;
            default -> throw new IllegalArgumentException(
                "unsupported format '" + name + "' — supported: canonical, lower, uuid, hex");
        };
    }
}
