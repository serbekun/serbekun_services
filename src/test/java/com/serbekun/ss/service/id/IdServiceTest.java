package com.serbekun.ss.service.id;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link IdService}.
 * <p>
 * The structural bits of a UUID — the version and variant nibbles — are checked
 * against the bytes rather than the string, because a generator that gets them
 * wrong still produces something that looks exactly like a UUID and is quietly
 * not one.
 */
class IdServiceTest {

    private final IdService service = new IdService();

    private static IdService.Spec spec(IdType type, Integer count, String version, String format,
            Integer length, String alphabet, String chars) {
        return new IdService.Spec(type, count, version, format, length, alphabet, chars);
    }

    private List<String> values(IdService.Spec spec) {
        return service.generate(spec).values();
    }

    // region uuid

    @Test
    void uuidV4IsRandomAndCorrectlyStamped() {
        IdService.Result result = service.generate(spec(IdType.UUID, 100, null, null, null, null, null));

        assertThat(result.type()).isEqualTo("uuid");
        assertThat(result.version()).isEqualTo("v4");
        assertThat(result.format()).isEqualTo("canonical");
        assertThat(result.bits()).isEqualTo(122);
        assertThat(result.count()).isEqualTo(100);
        assertThat(result.values()).hasSize(100).doesNotHaveDuplicates();

        for (String value : result.values()) {
            UUID parsed = UUID.fromString(value);
            assertThat(parsed.version()).isEqualTo(4);
            assertThat(parsed.variant()).isEqualTo(2); // the RFC 4122 0b10 variant
        }
    }

    @Test
    void uuidV7CarriesTheTimeItWasMadeAndSortsByIt() throws Exception {
        long before = System.currentTimeMillis();
        IdService.Result first = service.generate(spec(IdType.UUID, 1, "v7", null, null, null, null));
        Thread.sleep(5);
        IdService.Result second = service.generate(spec(IdType.UUID, 1, "7", null, null, null, null));
        long after = System.currentTimeMillis();

        assertThat(first.version()).isEqualTo("v7");
        assertThat(first.bits()).isEqualTo(74);

        UUID one = UUID.fromString(first.values().get(0));
        UUID two = UUID.fromString(second.values().get(0));
        assertThat(one.version()).isEqualTo(7);
        assertThat(one.variant()).isEqualTo(2);

        long timestamp = one.getMostSignificantBits() >>> 16;
        assertThat(timestamp).isBetween(before, after);

        // Time-ordered is the entire reason to pick v7: an id made later must
        // sort after one made earlier, as text and as bytes alike.
        assertThat(first.values().get(0)).isLessThan(second.values().get(0));
        assertThat(timestamp).isLessThan(two.getMostSignificantBits() >>> 16);
    }

    @Test
    void uuidFormatsAreTheSameValueWrittenFourWays() {
        String canonical = values(spec(IdType.UUID, 1, null, "canonical", null, null, null)).get(0);
        assertThat(canonical).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        assertThat(values(spec(IdType.UUID, 1, null, "compact", null, null, null)).get(0))
            .matches("[0-9a-f]{32}");
        assertThat(values(spec(IdType.UUID, 1, null, "upper", null, null, null)).get(0))
            .matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
        assertThat(values(spec(IdType.UUID, 1, null, "urn", null, null, null)).get(0))
            .startsWith("urn:uuid:")
            .hasSize("urn:uuid:".length() + 36);
    }

    // endregion

    // region ulid

    @Test
    void ulidIsTwentySixCharactersAndAscends() {
        IdService.Result result = service.generate(spec(IdType.ULID, 50, null, null, null, null, null));

        assertThat(result.type()).isEqualTo("ulid");
        assertThat(result.format()).isEqualTo("canonical");
        assertThat(result.bits()).isEqualTo(80);
        assertThat(result.values()).hasSize(50).doesNotHaveDuplicates().isSorted();
        assertThat(result.values()).allMatch(v -> v.matches("[0-9A-HJKMNP-TV-Z]{26}"));
    }

    @Test
    void ulidFormatsAreTheSameBitsWrittenFourWays() {
        assertThat(values(spec(IdType.ULID, 1, null, "lower", null, null, null)).get(0))
            .matches("[0-9a-hjkmnp-tv-z]{26}");
        assertThat(values(spec(IdType.ULID, 1, null, "hex", null, null, null)).get(0))
            .matches("[0-9a-f]{32}");

        // The same 128 bits fit a uuid column exactly, which is the point of the format.
        String asUuid = values(spec(IdType.ULID, 1, null, "uuid", null, null, null)).get(0);
        assertThat(UUID.fromString(asUuid)).isNotNull();
    }

    // endregion

    // region token

    @Test
    void tokensUseTheAlphabetAskedForAndReportTheirEntropy() {
        IdService.Result base62 = service.generate(spec(IdType.TOKEN, 10, null, null, null, null, null));

        assertThat(base62.type()).isEqualTo("token");
        assertThat(base62.alphabet()).isEqualTo("base62");
        assertThat(base62.length()).isEqualTo(32);
        // 32 characters of base62 is floor(32 × log2 62) = 190 bits.
        assertThat(base62.bits()).isEqualTo(190);
        assertThat(base62.values()).hasSize(10).doesNotHaveDuplicates();
        assertThat(base62.values()).allMatch(v -> v.matches("[0-9A-Za-z]{32}"));

        IdService.Result pin = service.generate(spec(IdType.TOKEN, 1, null, null, 6, "digits", null));
        assertThat(pin.values().get(0)).matches("[0-9]{6}");
        assertThat(pin.bits()).isEqualTo(19); // floor(6 × log2 10)

        IdService.Result readable = service.generate(spec(IdType.TOKEN, 5, null, null, 20, "base58", null));
        // base58 exists to drop the characters that look alike.
        assertThat(readable.values()).allMatch(v -> v.matches("[123456789A-HJ-NP-Za-km-z]{20}"));
    }

    @Test
    void aCustomAlphabetIsUsedAsGiven() {
        IdService.Result result = service.generate(spec(IdType.TOKEN, 20, null, null, 16, null, "ATGC"));

        assertThat(result.alphabet()).isEqualTo("custom");
        assertThat(result.bits()).isEqualTo(32); // 16 × log2 4
        assertThat(result.values()).allMatch(v -> v.matches("[ATGC]{16}"));
    }

    @Test
    void aCustomAlphabetThatWouldSkewTheDrawIsRejected() {
        // A repeated character is drawn twice as often, so the reported bits would lie.
        assertThatThrownBy(() -> service.generate(spec(IdType.TOKEN, 1, null, null, 8, null, "AABB")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not repeat a character");

        assertThatThrownBy(() -> service.generate(spec(IdType.TOKEN, 1, null, null, 8, null, "A")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 2 and 256 characters");
    }

    @Test
    void everyCharacterOfTheAlphabetCanComeUp() {
        // A modulo-based draw would starve the tail of an alphabet whose size
        // is not a power of two; over 20k draws from base62 all 62 must appear.
        IdService.Result result = service.generate(spec(IdType.TOKEN, 20, null, null, 1000, "base62", null));

        Set<Character> seen = new HashSet<>();
        result.values().forEach(v -> v.chars().forEach(c -> seen.add((char) c)));
        assertThat(seen).hasSize(62);
    }

    // endregion

    // region bytes

    @Test
    void randomBytesComeBackInTheFormatAskedFor() {
        IdService.Result hex = service.generate(spec(IdType.BYTES, 3, null, null, 16, null, null));

        assertThat(hex.type()).isEqualTo("bytes");
        assertThat(hex.format()).isEqualTo("hex");
        assertThat(hex.length()).isEqualTo(16);
        assertThat(hex.bits()).isEqualTo(128);
        assertThat(hex.values()).hasSize(3).doesNotHaveDuplicates();
        assertThat(hex.values()).allMatch(v -> v.matches("[0-9a-f]{32}"));

        String base64 = values(spec(IdType.BYTES, 1, null, "base64", 32, null, null)).get(0);
        assertThat(java.util.Base64.getDecoder().decode(base64)).hasSize(32);

        String base32 = values(spec(IdType.BYTES, 1, null, "base32", 10, null, null)).get(0);
        assertThat(base32).matches("[A-Z2-7]{16}");
    }

    @Test
    void randomBytesCannotBeAskedForAsText() {
        assertThatThrownBy(() -> service.generate(spec(IdType.BYTES, 1, null, "utf8", 16, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("random bytes are not text");
    }

    // endregion

    // region limits and batch

    @Test
    void countAndLengthAreBounded() {
        assertThatThrownBy(() -> service.generate(spec(IdType.UUID, 0, null, null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count must be between 1 and 1000");

        assertThatThrownBy(() -> service.generate(spec(IdType.UUID, 1001, null, null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count must be between 1 and 1000");

        assertThatThrownBy(() -> service.generate(spec(IdType.TOKEN, 1, null, null, 0, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("length must be between 1 and 4096");

        assertThatThrownBy(() -> service.generate(spec(IdType.BYTES, 1, null, null, 5000, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("length must be between 1 and 4096");
    }

    @Test
    void unknownChoicesAreRejectedByName() {
        assertThatThrownBy(() -> service.generate(spec(IdType.UUID, 1, "v9", null, null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported version 'v9'");

        assertThatThrownBy(() -> service.generate(spec(IdType.UUID, 1, null, "braces", null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported format 'braces'");

        assertThatThrownBy(() -> service.generate(spec(IdType.TOKEN, 1, null, null, 8, "emoji", null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported alphabet 'emoji'");

        assertThatThrownBy(() -> IdType.fromWire("snowflake"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported type 'snowflake'");
    }

    @Test
    void aBatchMixesKindsAndKeepsTheirOrder() {
        List<IdService.Result> results = service.batch(List.of(
            spec(IdType.UUID, 2, "v7", "compact", null, null, null),
            spec(IdType.ULID, 3, null, null, null, null, null),
            spec(IdType.TOKEN, 1, null, null, 12, "base58", null),
            spec(IdType.BYTES, 1, null, "base64url", 8, null, null)));

        assertThat(results).hasSize(4);
        assertThat(results.get(0).type()).isEqualTo("uuid");
        assertThat(results.get(0).values()).hasSize(2).allMatch(v -> v.matches("[0-9a-f]{32}"));
        assertThat(results.get(1).values()).hasSize(3).isSorted();
        assertThat(results.get(2).values().get(0)).hasSize(12);
        assertThat(results.get(3).values()).hasSize(1);
    }

    @Test
    void aBatchIsCappedOnItsTotalNotOnEachItem() {
        List<IdService.Spec> tooMany = List.of(
            spec(IdType.UUID, 600, null, null, null, null, null),
            spec(IdType.ULID, 600, null, null, null, null, null));

        assertThatThrownBy(() -> service.batch(tooMany))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at most 1000 values in total");

        assertThatThrownBy(() -> service.batch(List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one item is required");
    }

    @Test
    void theGeneratorIsSeededPerCall() {
        // Two services made from two SecureRandoms must not agree on anything.
        IdService one = new IdService(new SecureRandom());
        IdService two = new IdService(new SecureRandom());

        assertThat(one.generate(spec(IdType.TOKEN, 1, null, null, 32, null, null)).values())
            .isNotEqualTo(two.generate(spec(IdType.TOKEN, 1, null, null, 32, null, null)).values());
    }

    @Test
    void aPinnedRandomMakesTheDrawReproducible() {
        // Not a property of the service — a check that the draw really is the
        // injected randomness and nothing else, so the SecureRandom actually matters.
        assertThat(new IdService(pinned(42)).generate(spec(IdType.TOKEN, 1, null, null, 24, null, null)).values())
            .isEqualTo(new IdService(pinned(42)).generate(spec(IdType.TOKEN, 1, null, null, 24, null, null)).values());
    }

    /** A SecureRandom that is not secure at all, so a test can know what comes next. */
    private static SecureRandom pinned(long seed) {
        return new SecureRandom() {
            private final Random delegate = new Random(seed);

            @Override
            public int nextInt(int bound) {
                return delegate.nextInt(bound);
            }

            @Override
            public void nextBytes(byte[] bytes) {
                delegate.nextBytes(bytes);
            }
        };
    }
}
