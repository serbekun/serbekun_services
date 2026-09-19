package com.serbekun.ss.service.burnlink;

import java.util.List;
import java.util.Locale;

/**
 * Best-effort classification of a {@code User-Agent} string into a device type
 * and a browser family.
 * <p>
 * Only the coarse vocabulary the burn-link feature exposes is reported —
 * {@code iphone, ipad, android, windows, mac, linux, other} and
 * {@code edge, opera, firefox, chrome, safari, other} — because the point is to
 * express "iPhone yes, Android no", not to fingerprint anyone.
 * <p>
 * Order matters: Edge and Opera both claim to be Chrome, and Chrome claims to
 * be Safari, so the more specific token is tested first. iPads report
 * "Mac OS X" in their UA, so {@code ipad} is tested before {@code mac}.
 */
public final class UserAgentClassifier {

    public static final String OTHER = "other";

    /** Device values accepted by the API, in display order. */
    public static final List<String> DEVICES =
            List.of("iphone", "ipad", "android", "windows", "mac", "linux", OTHER);

    /** Browser values accepted by the API, in display order. */
    public static final List<String> BROWSERS =
            List.of("edge", "opera", "firefox", "chrome", "safari", OTHER);

    private UserAgentClassifier() {
    }

    /**
     * @param userAgent the raw {@code User-Agent} header, may be null
     * @return the detected device type, never null
     */
    public static String device(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("iphone") || ua.contains("ipod")) return "iphone";
        if (ua.contains("ipad")) return "ipad";
        if (ua.contains("android")) return "android";
        if (ua.contains("windows")) return "windows";
        if (ua.contains("macintosh") || ua.contains("mac os x")) return "mac";
        if (ua.contains("linux") || ua.contains("x11")) return "linux";
        return OTHER;
    }

    /**
     * @param userAgent the raw {@code User-Agent} header, may be null
     * @return the detected browser family, never null
     */
    public static String browser(String userAgent) {
        String ua = lower(userAgent);
        if (ua.contains("edg/") || ua.contains("edga/") || ua.contains("edgios") || ua.contains("edge")) {
            return "edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) return "opera";
        if (ua.contains("firefox") || ua.contains("fxios")) return "firefox";
        if (ua.contains("crios") || ua.contains("chrome")) return "chrome";
        if (ua.contains("safari")) return "safari";
        return OTHER;
    }

    /**
     * Checks a value against an allow list. An empty or null list means "no
     * restriction".
     *
     * @param value  the detected value
     * @param allow  the allowed values, or empty for all
     * @return true when access is allowed
     */
    public static boolean allowed(String value, List<String> allow) {
        return allow == null || allow.isEmpty() || allow.contains(value);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
