package com.serbekun.ss.service.id;

import java.util.Locale;

/**
 * The character sets a random token can be drawn from.
 * <p>
 * Which one to pick is a question about where the token has to survive:
 * {@link #BASE58} drops the characters that look alike ({@code 0OIl}) and is
 * the one to use if a human will ever read a token aloud or copy it off a
 * screen; {@link #BASE64URL} packs the most entropy per character into
 * something still safe in a url; {@link #HEX} is the one every tool can parse.
 */
public enum TokenAlphabet {

    BASE62("base62", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"),
    BASE58("base58", "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"),
    BASE64URL("base64url", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"),
    BASE32("base32", "0123456789ABCDEFGHJKMNPQRSTVWXYZ"),
    HEX("hex", "0123456789abcdef"),
    DIGITS("digits", "0123456789"),
    LOWER("lower", "abcdefghijklmnopqrstuvwxyz"),
    UPPER("upper", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");

    private final String wireName;
    private final String characters;

    TokenAlphabet(String wireName, String characters) {
        this.wireName = wireName;
        this.characters = characters;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /** The characters a token is drawn from. */
    public String characters() {
        return characters;
    }

    /**
     * Resolves a caller-supplied alphabet name.
     *
     * @param name the alphabet as written by the caller; blank means {@link #BASE62}
     * @return the matching alphabet
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static TokenAlphabet fromWire(String name) {
        if (name == null || name.isBlank()) {
            return BASE62;
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return switch (normalized) {
            case "base62", "alphanumeric", "alnum", "default" -> BASE62;
            case "base58", "bitcoin" -> BASE58;
            case "base64url", "base64", "urlsafe", "b64url" -> BASE64URL;
            case "base32", "crockford" -> BASE32;
            case "hex", "base16" -> HEX;
            case "digits", "numeric", "pin" -> DIGITS;
            case "lower", "lowercase", "alpha" -> LOWER;
            case "upper", "uppercase" -> UPPER;
            default -> throw new IllegalArgumentException(
                "unsupported alphabet '" + name + "' — supported: " + supported()
                    + ", or pass your own set of characters as 'chars'");
        };
    }

    /** Comma separated list of the canonical names, for error messages. */
    public static String supported() {
        StringBuilder names = new StringBuilder();
        for (TokenAlphabet alphabet : values()) {
            if (names.length() > 0) {
                names.append(", ");
            }
            names.append(alphabet.wireName);
        }
        return names.toString();
    }
}
