package com.serbekun.ss.service.encoding;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Percent-encoding, in the two variants that are actually in use.
 * <p>
 * RFC 3986 escapes everything outside the unreserved set and leaves {@code +}
 * alone; {@code application/x-www-form-urlencoded} writes a space as {@code +}
 * and therefore has to escape a literal plus. Mixing the two is the classic way
 * to turn "a+b" into "a b", so which one is in play is always explicit here.
 */
final class PercentCodec {

    /** RFC 3986 unreserved characters — the ones that never need escaping. */
    private static final String UNRESERVED =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private PercentCodec() {
    }

    /**
     * Percent-encodes bytes.
     *
     * @param data the bytes to encode
     * @param formStyle true for {@code x-www-form-urlencoded} (space becomes {@code +})
     * @return the encoded text
     */
    static String encode(byte[] data, boolean formStyle) {
        StringBuilder out = new StringBuilder(data.length * 3);

        for (byte raw : data) {
            char c = (char) (raw & 0xFF);
            if (UNRESERVED.indexOf(c) >= 0) {
                out.append(c);
            } else if (formStyle && c == ' ') {
                out.append('+');
            } else {
                out.append('%')
                   .append(HEX_DIGITS[(raw >> 4) & 0xF])
                   .append(HEX_DIGITS[raw & 0xF]);
            }
        }

        return out.toString();
    }

    /**
     * Decodes percent-encoded text.
     * <p>
     * A character that was never escaped contributes its own UTF-8 bytes, so
     * text pasted in half-encoded still decodes to what it looks like.
     *
     * @param text the encoded text
     * @param formStyle true for {@code x-www-form-urlencoded} ({@code +} becomes a space)
     * @return the decoded bytes
     * @throws IllegalArgumentException when a {@code %} is not followed by two hex digits
     */
    static byte[] decode(String text, boolean formStyle) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(text.length());

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '%') {
                if (i + 2 >= text.length()) {
                    throw new IllegalArgumentException(
                        "data is not valid url encoding: '%' at the end needs two hex digits after it");
                }
                int high = Character.digit(text.charAt(i + 1), 16);
                int low = Character.digit(text.charAt(i + 2), 16);
                if (high < 0 || low < 0) {
                    throw new IllegalArgumentException(
                        "data is not valid url encoding: '%" + text.charAt(i + 1) + text.charAt(i + 2)
                            + "' is not a hex escape");
                }
                out.write((high << 4) | low);
                i += 2;
            } else if (formStyle && c == '+') {
                out.write(' ');
            } else {
                out.writeBytes(String.valueOf(c).getBytes(StandardCharsets.UTF_8));
            }
        }

        return out.toByteArray();
    }
}
