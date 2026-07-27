package com.serbekun.ss.service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Helpers for handling secret values (tokens) safely.
 */
public final class Secrets {

    private Secrets() {}

    /**
     * Compares two secret strings in length-constant time to avoid leaking
     * information through timing side-channels. Both arguments may be null;
     * a null never matches a non-null value.
     *
     * @param a first secret (e.g. the token supplied by the client)
     * @param b second secret (e.g. the token stored on the server)
     * @return true only if both are non-null and equal
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8));
    }
}
