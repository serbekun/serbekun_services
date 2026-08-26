package com.serbekun.ss.service.qr;

import java.util.Locale;

/**
 * The image formats a generated QR code can come back in.
 * <p>
 * PNG is raster and prints or pastes anywhere; SVG stays sharp at any size and
 * is the one to embed in a page or hand to a printer. Both are produced from
 * the same module matrix, so the code itself is identical.
 */
public enum QrFormat {

    PNG("png", "image/png"),
    SVG("svg", "image/svg+xml");

    private final String wireName;
    private final String contentType;

    QrFormat(String wireName, String contentType) {
        this.wireName = wireName;
        this.contentType = contentType;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /** The MIME type the image is served with. */
    public String contentType() {
        return contentType;
    }

    /**
     * Resolves a caller-supplied format name.
     *
     * @param name the format as written by the caller; blank means {@link #PNG}
     * @return the matching format
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static QrFormat fromWire(String name) {
        if (name == null || name.isBlank()) {
            return PNG;
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("image/")) {
            normalized = normalized.substring("image/".length());
        }
        if (normalized.equals("svg+xml")) {
            normalized = "svg";
        }

        for (QrFormat format : values()) {
            if (format.wireName.equals(normalized)) {
                return format;
            }
        }
        throw new IllegalArgumentException("unsupported format '" + name + "' — supported: " + supported());
    }

    /** Comma separated list of the canonical names, for error messages. */
    public static String supported() {
        StringBuilder names = new StringBuilder();
        for (QrFormat format : values()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(format.wireName);
        }
        return names.toString();
    }
}
