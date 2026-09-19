package com.serbekun.ss.service.burnlink;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Matching of client IP addresses against allow and deny lists.
 * <p>
 * Entries may be a single IPv4/IPv6 address or a CIDR block such as
 * {@code 192.168.0.0/24} or {@code 2001:db8::/32}. The blacklist is always
 * evaluated first, so an address that appears in both lists is blocked. An
 * empty whitelist means "allow any address".
 */
public final class IpAccessList {

    /** Characters allowed in an IP or CIDR entry. */
    private static final Pattern ENTRY_PATTERN = Pattern.compile("[0-9a-fA-F:.%/]+");

    private IpAccessList() {
    }

    /**
     * @param clientIp  the resolved client address, may be null
     * @param whitelist allowed addresses/CIDRs, empty means all
     * @param blacklist denied addresses/CIDRs, evaluated first
     * @return true when the address may access the link
     */
    public static boolean isAllowed(String clientIp, List<String> whitelist, List<String> blacklist) {
        if (matchesAny(clientIp, blacklist)) {
            return false;
        }
        return whitelist == null || whitelist.isEmpty() || matchesAny(clientIp, whitelist);
    }

    /**
     * @param clientIp the resolved client address, may be null
     * @param entries  addresses/CIDRs to match against
     * @return true when the address matches at least one entry
     */
    public static boolean matchesAny(String clientIp, List<String> entries) {
        if (clientIp == null || entries == null || entries.isEmpty()) {
            return false;
        }
        InetAddress ip = parse(normalize(clientIp));
        if (ip == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            if (matches(ip, entry.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates a single list entry so bad input is rejected at creation time
     * rather than silently never matching.
     *
     * @param entry the address or CIDR to validate
     * @return true when the entry is a parsable IP or CIDR block
     */
    public static boolean isValidEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }
        String value = entry.trim();
        if (!ENTRY_PATTERN.matcher(value).matches()) {
            return false;
        }
        int slash = value.indexOf('/');
        if (slash < 0) {
            return parse(value) != null;
        }
        String address = value.substring(0, slash);
        String prefix = value.substring(slash + 1);
        if (parse(address) == null || prefix.isEmpty()) {
            return false;
        }
        try {
            int bits = Integer.parseInt(prefix);
            return bits >= 0 && bits <= parse(address).getAddress().length * 8;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean matches(InetAddress ip, String entry) {
        int slash = entry.indexOf('/');
        if (slash < 0) {
            InetAddress expected = parse(entry);
            return expected != null && ip.equals(expected);
        }
        String address = entry.substring(0, slash);
        String prefixText = entry.substring(slash + 1);
        InetAddress network = parse(address);
        if (network == null) {
            return false;
        }
        int prefix;
        try {
            prefix = Integer.parseInt(prefixText);
        } catch (NumberFormatException e) {
            return false;
        }
        byte[] ipBytes = ip.getAddress();
        byte[] netBytes = network.getAddress();
        if (ipBytes.length != netBytes.length) {
            return false;
        }
        if (prefix < 0 || prefix > ipBytes.length * 8) {
            return false;
        }

        int fullBytes = prefix / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (ipBytes[i] != netBytes[i]) {
                return false;
            }
        }
        int remainder = prefix % 8;
        if (remainder != 0) {
            int mask = (0xFF << (8 - remainder)) & 0xFF;
            if ((ipBytes[fullBytes] & mask) != (netBytes[fullBytes] & mask)) {
                return false;
            }
        }
        return true;
    }

    private static InetAddress parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String host = normalize(value);
        if (host.isEmpty() || !ENTRY_PATTERN.matcher(host).matches()) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            // getByName resolves host names; the character guard above already
            // restricts input to literals, and a numeric-only host that is not
            // an IP (e.g. "999.1.1.1") falls through to null.
            return address;
        } catch (Exception e) {
            return null;
        }
    }

    /** Strips brackets and an optional {@code :port} from an IPv4 address. */
    private static String normalize(String value) {
        String host = value.trim().toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon && host.indexOf('.') >= 0) {
            host = host.substring(0, colon);
        }
        return host;
    }
}
