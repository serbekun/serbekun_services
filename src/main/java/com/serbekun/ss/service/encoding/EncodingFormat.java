package com.serbekun.ss.service.encoding;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * The representations a payload can be written in.
 * <p>
 * Every format is a two-way street between text and bytes, which is what makes
 * one converter enough for all of them: decode from whatever the caller sent,
 * encode into whatever they asked for. Encoding always produces the canonical
 * form; decoding is forgiving about the ways the same data gets pasted around —
 * lowercase, whitespace, absent padding.
 */
public enum EncodingFormat {

    /** Plain text. The bytes are its UTF-8 encoding. */
    UTF8("utf8"),

    /** RFC 4648 base64, padded. Decoding also accepts the url-safe alphabet. */
    BASE64("base64"),

    /** RFC 4648 url-safe base64 ({@code -} and {@code _}), written without padding. */
    BASE64URL("base64url"),

    /** RFC 4648 base32, uppercase and padded. */
    BASE32("base32"),

    /** Lowercase hex. Decoding accepts uppercase, whitespace, colons and an {@code 0x} prefix. */
    HEX("hex"),

    /** RFC 3986 percent-encoding: a space is {@code %20} and {@code +} is a literal plus. */
    URL("url"),

    /** {@code application/x-www-form-urlencoded}: a space is {@code +}. */
    URL_FORM("url-form");

    private final String wireName;

    EncodingFormat(String wireName) {
        this.wireName = wireName;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /**
     * Writes bytes in this format.
     *
     * @param data the bytes to write
     * @return the encoded text
     * @throws IllegalArgumentException when the bytes are not valid UTF-8 and
     *         this format is {@link #UTF8}
     */
    public String encode(byte[] data) {
        return switch (this) {
            case UTF8 -> utf8(data);
            case BASE64 -> Base64.getEncoder().encodeToString(data);
            case BASE64URL -> Base64.getUrlEncoder().withoutPadding().encodeToString(data);
            case BASE32 -> Base32.encode(data);
            case HEX -> HexFormat.of().formatHex(data);
            case URL -> PercentCodec.encode(data, false);
            case URL_FORM -> PercentCodec.encode(data, true);
        };
    }

    /**
     * Reads text written in this format.
     *
     * @param data the text to read
     * @return the bytes it stands for
     * @throws IllegalArgumentException when the text is not valid in this format
     */
    public byte[] decode(String data) {
        return switch (this) {
            case UTF8 -> data.getBytes(StandardCharsets.UTF_8);
            case BASE64 -> base64(data);
            case BASE64URL -> base64Url(data);
            case BASE32 -> Base32.decode(data);
            case HEX -> hex(data);
            case URL -> PercentCodec.decode(data, false);
            case URL_FORM -> PercentCodec.decode(data, true);
        };
    }

    /**
     * Resolves a caller-supplied format name.
     * <p>
     * Spelling is matched loosely — {@code base-64}, {@code BASE_64} and
     * {@code base64} are one name — and a few everyday aliases are accepted
     * ({@code text}, {@code b64}, {@code base16}, {@code percent}).
     *
     * @param name the format as written by the caller
     * @param field the field name, so an error says which side was wrong
     * @return the matching format
     * @throws IllegalArgumentException when the name is blank or matches nothing
     */
    public static EncodingFormat fromWire(String name, String field) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(field + " is required — supported: " + supported());
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT)
            .replace("-", "").replace("_", "").replace(" ", "");

        return switch (normalized) {
            case "utf8", "text", "plain", "string", "raw" -> UTF8;
            case "base64", "b64" -> BASE64;
            case "base64url", "b64url", "base64urlsafe", "urlsafebase64" -> BASE64URL;
            case "base32", "b32" -> BASE32;
            case "hex", "base16", "hexadecimal" -> HEX;
            case "url", "percent", "urlencoded", "rfc3986" -> URL;
            case "urlform", "form", "formurlencoded", "xwwwformurlencoded" -> URL_FORM;
            default -> throw new IllegalArgumentException(
                "unsupported " + field + " '" + name + "' — supported: " + supported());
        };
    }

    /** Comma separated list of the canonical names, for error messages. */
    public static String supported() {
        StringBuilder names = new StringBuilder();
        for (EncodingFormat format : values()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(format.wireName);
        }
        return names.toString();
    }

    // region decoders

    /**
     * Renders bytes as text, refusing rather than mangling.
     * <p>
     * The default decoder would quietly replace anything that is not valid
     * UTF-8 with '�', which loses the data and hides that it happened — so
     * arbitrary bytes are reported as needing a different output format instead.
     */
    private static String utf8(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(data))
                .toString();
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException(
                "the data is not valid UTF-8 text — ask for hex, base64 or base32 output instead");
        }
    }

    /** Strict base64, after forgiving whitespace and the url-safe alphabet. */
    private static byte[] base64(String data) {
        // A JWT segment or a url-safe string pasted in here means exactly what
        // it says, so it is translated rather than rejected on the alphabet.
        String cleaned = data.replaceAll("\\s", "").replace('-', '+').replace('_', '/');
        try {
            return Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("data is not valid base64");
        }
    }

    private static byte[] base64Url(String data) {
        String cleaned = data.replaceAll("\\s", "").replace('+', '-').replace('/', '_');
        while (cleaned.endsWith("=")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        try {
            return Base64.getUrlDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("data is not valid base64url");
        }
    }

    /** Hex, after forgiving the separators a dump tends to carry. */
    private static byte[] hex(String data) {
        String cleaned = data.replaceAll("[\\s:,-]", "");
        if (cleaned.length() >= 2 && cleaned.regionMatches(true, 0, "0x", 0, 2)) {
            cleaned = cleaned.substring(2);
        }
        if (cleaned.length() % 2 != 0) {
            throw new IllegalArgumentException("data is not valid hex: it has an odd number of digits");
        }
        try {
            return HexFormat.of().parseHex(cleaned);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("data is not valid hex");
        }
    }

    // endregion
}
