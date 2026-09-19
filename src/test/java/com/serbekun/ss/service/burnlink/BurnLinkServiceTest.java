package com.serbekun.ss.service.burnlink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.serbekun.ss.domain.models.BurnLink;
import com.serbekun.ss.repo.burnlink.BurnLinkRepo;

class BurnLinkServiceTest {

    private static final String IPHONE_SAFARI =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
            + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    private static final String ANDROID_CHROME =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";
    private static final String WINDOWS_FIREFOX =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0";
    private static final String WINDOWS_EDGE =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0 Safari/537.36 Edg/120.0";

    private BurnLinkRepo repo;
    private BurnLinkService service;

    @BeforeEach
    void setUp() {
        repo = new BurnLinkRepo(new HashMap<>());
        service = new BurnLinkService(repo);
    }

    private BurnLink create(String text, long ttl, List<String> devices, List<String> browsers) {
        return service.create(text, null, ttl, devices, browsers, null, null);
    }

    // region create

    @Test
    void createStoresLinkWithFreshToken() {
        BurnLink link = create("hello", 0, null, null);

        assertThat(link.id()).matches("[A-Za-z0-9]{10}");
        assertThat(link.token()).isNotBlank();
        assertThat(link.expiredTime()).isZero();
        assertThat(repo.getBurnLink(link.id())).isEqualTo(link);
    }

    @Test
    void createComputesExpiredTimeFromTtl() {
        long before = System.currentTimeMillis();
        BurnLink link = create("hello", 60, null, null);

        assertThat(link.expiredTime()).isBetween(before + 59_000, before + 61_000);
    }

    @Test
    void createRejectsBadInput() {
        assertThatThrownBy(() -> create("", 0, null, null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("text");
        assertThatThrownBy(() -> create("   ", 0, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> create("hello", -1, null, null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ttl");
        assertThatThrownBy(() -> create("hello", 0, List.of("blackberry"), null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("device");
        assertThatThrownBy(() -> create("hello", 0, null, List.of("netscape")))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("browser");
    }

    @Test
    void createRejectsOversizedText() {
        String tooLong = "x".repeat(BurnLinkService.MAX_TEXT_LENGTH + 1);
        assertThatThrownBy(() -> create(tooLong, 0, null, null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exceed");
    }

    // endregion

    // region reveal

    @Test
    void resolveDoesNotBurnButRevealDoesItOnce() {
        BurnLink link = create("secret", 0, null, null);

        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "1.2.3.4")).isNotNull();
        assertThat(repo.getBurnLink(link.id())).isNotNull();

        assertThat(service.reveal(link.id(), IPHONE_SAFARI, "1.2.3.4")).isEqualTo("secret");
        assertThat(repo.getBurnLink(link.id())).isNull();
        assertThat(service.reveal(link.id(), IPHONE_SAFARI, "1.2.3.4")).isNull();
    }

    @Test
    void unknownLinkResolvesAndRevealsToNull() {
        assertThat(service.resolve("doesnotexist", IPHONE_SAFARI, "1.2.3.4")).isNull();
        assertThat(service.reveal("doesnotexist", IPHONE_SAFARI, "1.2.3.4")).isNull();
    }

    // endregion

    // region whitelists

    @Test
    void emptyDeviceListAllowsAnyDevice() {
        BurnLink link = create("secret", 0, List.of(), List.of());

        assertThat(service.resolve(link.id(), ANDROID_CHROME, "1.2.3.4")).isNotNull();
    }

    @Test
    void deviceWhitelistBlocksOtherDevicesWithoutBurning() {
        BurnLink link = create("secret", 0, List.of("iphone"), null);

        assertThat(service.reveal(link.id(), ANDROID_CHROME, "1.2.3.4")).isNull();
        assertThat(repo.getBurnLink(link.id())).isNotNull();
        assertThat(service.reveal(link.id(), IPHONE_SAFARI, "1.2.3.4")).isEqualTo("secret");
    }

    @Test
    void browserWhitelistBlocksOtherBrowsers() {
        BurnLink link = create("secret", 0, null, List.of("firefox"));

        assertThat(service.reveal(link.id(), WINDOWS_EDGE, "1.2.3.4")).isNull();
        assertThat(service.reveal(link.id(), WINDOWS_FIREFOX, "1.2.3.4")).isEqualTo("secret");
    }

    // endregion

    // region ip lists

    @Test
    void ipBlacklistAlwaysBlocks() {
        BurnLink link = service.create("secret", null, 0, null, null,
                null, List.of("203.0.113.0/24"));

        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "203.0.113.9")).isNull();
        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "203.0.114.9")).isNotNull();
    }

    @Test
    void ipWhitelistRestrictsAndBlacklistWins() {
        BurnLink link = service.create("secret", null, 0, null, null,
                List.of("203.0.113.0/24"), List.of("203.0.113.7"));

        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "203.0.113.7")).isNull();
        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "203.0.113.8")).isNotNull();
        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "198.51.100.1")).isNull();
    }

    @Test
    void emptyIpWhitelistAllowsAnyAddress() {
        BurnLink link = service.create("secret", null, 0, null, null, List.of(), List.of());

        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "198.51.100.1")).isNotNull();
    }

    @Test
    void createRejectsInvalidIpEntries() {
        assertThatThrownBy(() -> service.create("secret", null, 0, null, null,
                List.of("not-an-ip"), null))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ipWhitelist");
    }

    // endregion

    // region expiry + delete

    @Test
    void expiredLinkResolvesToNullAndIsRemoved() {
        long past = System.currentTimeMillis() - 1000;
        BurnLink link = new BurnLink("expired", "secret", "token", past, past,
                List.of(), List.of(), List.of(), List.of());
        repo.addBurnLink(link);

        assertThat(service.resolve(link.id(), IPHONE_SAFARI, "1.2.3.4")).isNull();
        assertThat(service.reveal(link.id(), IPHONE_SAFARI, "1.2.3.4")).isNull();
        assertThat(repo.getBurnLink(link.id())).isNull();
    }

    @Test
    void deleteRequiresToken() {
        BurnLink link = create("secret", 0, null, null);

        assertThat(service.delete(link.id(), "wrong")).isEqualTo(403);
        assertThat(service.delete(link.id(), null)).isEqualTo(403);
        assertThat(service.delete("nosuchlink", "any")).isEqualTo(404);
        assertThat(service.delete(link.id(), link.token())).isEqualTo(204);
        assertThat(repo.getBurnLink(link.id())).isNull();
    }

    @Test
    void deleteExpiredRemovesOnlyExpiredEntries() {
        long past = System.currentTimeMillis() - 1000;
        long future = System.currentTimeMillis() + 60_000;
        BurnLink expired = new BurnLink("a", "s", "t", past, past,
                List.of(), List.of(), List.of(), List.of());
        BurnLink eternal = new BurnLink("b", "s", "t", past, 0,
                List.of(), List.of(), List.of(), List.of());
        BurnLink fresh = new BurnLink("c", "s", "t", past, future,
                List.of(), List.of(), List.of(), List.of());
        repo.addBurnLink(expired);
        repo.addBurnLink(eternal);
        repo.addBurnLink(fresh);

        assertThat(service.deleteExpired()).isEqualTo(1);
        assertThat(repo.getBurnLink("a")).isNull();
        assertThat(repo.getBurnLink("b")).isNotNull();
        assertThat(repo.getBurnLink("c")).isNotNull();
    }

    // endregion
}
