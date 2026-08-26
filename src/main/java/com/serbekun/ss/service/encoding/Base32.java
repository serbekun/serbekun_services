package com.serbekun.ss.service.encoding;

import java.util.Locale;

/**
 * RFC 4648 base32, the one alphabet in this service the JDK does not ship.
 * <p>
 * Encoding produces the canonical form: uppercase, padded to a multiple of
 * eight characters. Decoding is deliberately looser — lowercase, absent
 * padding and whitespace all appear in the wild, and none of them change what
 * the data means.
 */
final class Base32 {

    /** The RFC 4648 alphabet: A–Z then 2–7. */
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    /** Reverse lookup, indexed by ASCII code; -1 marks a character that is not in the alphabet. */
    private static final int[] LOOKUP = new int[128];

    static {
        java.util.Arrays.fill(LOOKUP, -1);
        for (int i = 0; i < ALPHABET.length(); i++) {
            LOOKUP[ALPHABET.charAt(i)] = i;
            LOOKUP[Character.toLowerCase(ALPHABET.charAt(i))] = i;
        }
    }

    private Base32() {
    }

    /**
     * Encodes bytes as padded, uppercase base32.
     *
     * @param data the bytes to encode
     * @return the encoded text
     */
    static String encode(byte[] data) {
        StringBuilder out = new StringBuilder((data.length + 4) / 5 * 8);

        // Five bytes become eight characters, so the input is walked in
        // groups of five and each group is drained five bits at a time.
        for (int i = 0; i < data.length; i += 5) {
            int chunk = Math.min(5, data.length - i);

            long buffer = 0;
            for (int b = 0; b < 5; b++) {
                buffer <<= 8;
                if (b < chunk) {
                    buffer |= data[i + b] & 0xFF;
                }
            }

            // A partial group still fills whole characters: 1 byte → 2 chars,
            // 2 → 4, 3 → 5, 4 → 7. The rest of the block is padding.
            int characters = (chunk * 8 + 4) / 5;
            for (int c = 0; c < 8; c++) {
                if (c < characters) {
                    out.append(ALPHABET.charAt((int) ((buffer >>> (35 - c * 5)) & 0x1F)));
                } else {
                    out.append('=');
                }
            }
        }

        return out.toString();
    }

    /**
     * Decodes base32, tolerating lowercase, whitespace and missing padding.
     *
     * @param text the encoded text
     * @return the decoded bytes
     * @throws IllegalArgumentException when a character is not in the alphabet,
     *         or the text holds a number of characters no byte count could produce
     */
    static byte[] decode(String text) {
        String cleaned = text.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
        while (cleaned.endsWith("=")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) {
            return new byte[0];
        }

        // 1, 3 and 6 characters over a block cannot come from any whole
        // number of bytes, so they are a truncated string rather than a short one.
        int remainder = cleaned.length() % 8;
        if (remainder == 1 || remainder == 3 || remainder == 6) {
            throw new IllegalArgumentException("data is not valid base32: it ends mid-byte");
        }

        byte[] out = new byte[cleaned.length() * 5 / 8];
        int written = 0;
        long buffer = 0;
        int bits = 0;

        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            int value = c < LOOKUP.length ? LOOKUP[c] : -1;
            if (value < 0) {
                throw new IllegalArgumentException("data is not valid base32: '" + c + "' is not in the alphabet");
            }

            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                out[written++] = (byte) ((buffer >>> bits) & 0xFF);
            }
        }

        return out;
    }
}
