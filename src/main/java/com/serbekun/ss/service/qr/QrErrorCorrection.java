package com.serbekun.ss.service.qr;

import java.util.Locale;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * How much of the code may be damaged and still read: {@code L} tolerates ~7%,
 * {@code M} ~15%, {@code Q} ~25%, {@code H} ~30%.
 * <p>
 * Redundancy costs modules, so a higher level makes a denser code for the same
 * payload. {@code M} is the default because it survives ordinary printing and
 * a logo-free code without inflating the size.
 */
public enum QrErrorCorrection {

    L("L", ErrorCorrectionLevel.L),
    M("M", ErrorCorrectionLevel.M),
    Q("Q", ErrorCorrectionLevel.Q),
    H("H", ErrorCorrectionLevel.H);

    private final String wireName;
    private final ErrorCorrectionLevel level;

    QrErrorCorrection(String wireName, ErrorCorrectionLevel level) {
        this.wireName = wireName;
        this.level = level;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /** The ZXing level this maps to. */
    public ErrorCorrectionLevel level() {
        return level;
    }

    /**
     * Resolves a caller-supplied level name.
     *
     * @param name the level as written by the caller; blank means {@link #M}
     * @return the matching level
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static QrErrorCorrection fromWire(String name) {
        if (name == null || name.isBlank()) {
            return M;
        }

        String normalized = name.strip().toUpperCase(Locale.ROOT);
        for (QrErrorCorrection correction : values()) {
            if (correction.wireName.equals(normalized)) {
                return correction;
            }
        }
        throw new IllegalArgumentException(
            "unsupported errorCorrection '" + name + "' — supported: " + supported());
    }

    /** Comma separated list of the canonical names, for error messages. */
    public static String supported() {
        return "L, M, Q, H";
    }
}
