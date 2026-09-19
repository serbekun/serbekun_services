package com.serbekun.ss.service.burnlink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class IpAccessListTest {

    @Test
    void exactAddressesMatch() {
        assertThat(IpAccessList.matchesAny("203.0.113.7", List.of("203.0.113.7"))).isTrue();
        assertThat(IpAccessList.matchesAny("203.0.113.8", List.of("203.0.113.7"))).isFalse();
    }

    @Test
    void ipv4CidrBlocksMatch() {
        assertThat(IpAccessList.matchesAny("192.168.1.5", List.of("192.168.1.0/24"))).isTrue();
        assertThat(IpAccessList.matchesAny("192.168.2.5", List.of("192.168.1.0/24"))).isFalse();
        assertThat(IpAccessList.matchesAny("10.1.2.3", List.of("10.0.0.0/8"))).isTrue();
    }

    @Test
    void ipv6CidrBlocksMatch() {
        assertThat(IpAccessList.matchesAny("2001:db8::5", List.of("2001:db8::/32"))).isTrue();
        assertThat(IpAccessList.matchesAny("2001:dead::5", List.of("2001:db8::/32"))).isFalse();
    }

    @Test
    void blacklistIsEvaluatedBeforeWhitelist() {
        assertThat(IpAccessList.isAllowed("203.0.113.7",
                List.of("203.0.113.0/24"), List.of("203.0.113.7"))).isFalse();
        assertThat(IpAccessList.isAllowed("203.0.113.8",
                List.of("203.0.113.0/24"), List.of("203.0.113.7"))).isTrue();
    }

    @Test
    void emptyWhitelistAllowsAnythingExceptBlacklisted() {
        assertThat(IpAccessList.isAllowed("198.51.100.1", List.of(), List.of())).isTrue();
        assertThat(IpAccessList.isAllowed("198.51.100.1", null, List.of("198.51.100.0/24"))).isFalse();
    }

    @Test
    void invalidEntriesDoNotMatch() {
        assertThat(IpAccessList.matchesAny("203.0.113.7", List.of("not-an-ip"))).isFalse();
        assertThat(IpAccessList.matchesAny(null, List.of("203.0.113.7"))).isFalse();
    }

    @Test
    void validatesEntries() {
        assertThat(IpAccessList.isValidEntry("203.0.113.7")).isTrue();
        assertThat(IpAccessList.isValidEntry("203.0.113.0/24")).isTrue();
        assertThat(IpAccessList.isValidEntry("2001:db8::/32")).isTrue();
        assertThat(IpAccessList.isValidEntry("203.0.113.0/33")).isFalse();
        assertThat(IpAccessList.isValidEntry("example.com")).isFalse();
        assertThat(IpAccessList.isValidEntry("")).isFalse();
    }
}
