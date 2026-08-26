package com.serbekun.ss.service.json;

import java.util.Locale;

/**
 * The two ways to point at something inside a document.
 * <p>
 * {@link #POINTER} is RFC 6901: one path, at most one match, and the exact
 * syntax JSON Patch uses — so a pointer that finds a value is also the pointer
 * that would change it. {@link #JSONPATH} is the query language: wildcards,
 * slices and filters, many matches.
 */
public enum JsonQuerySyntax {

    POINTER("pointer"),
    JSONPATH("jsonpath");

    private final String wireName;

    JsonQuerySyntax(String wireName) {
        this.wireName = wireName;
    }

    /** The canonical spelling used in requests and echoed in responses. */
    public String wireName() {
        return wireName;
    }

    /**
     * Resolves a caller-supplied syntax name, or works it out from the
     * expression when none was given.
     *
     * @param name the syntax as written by the caller; blank means "detect it"
     * @param expression the expression to detect from
     * @return the syntax to use
     * @throws IllegalArgumentException when the name matches nothing supported
     */
    public static JsonQuerySyntax resolve(String name, String expression) {
        if (name == null || name.isBlank()) {
            return detect(expression);
        }

        String normalized = name.strip().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return switch (normalized) {
            case "pointer", "jsonpointer", "rfc6901" -> POINTER;
            case "jsonpath", "path" -> JSONPATH;
            default -> throw new IllegalArgumentException(
                "unsupported syntax '" + name + "' — supported: pointer, jsonpath");
        };
    }

    /**
     * Guesses the syntax from the expression itself: only JSONPath starts with
     * {@code $}, and only a pointer starts with {@code /} or is empty. The
     * shapes do not overlap, so the guess is safe — and a caller who disagrees
     * can always say which one they meant.
     */
    private static JsonQuerySyntax detect(String expression) {
        String trimmed = expression == null ? "" : expression.strip();
        return trimmed.startsWith("$") ? JSONPATH : POINTER;
    }
}
