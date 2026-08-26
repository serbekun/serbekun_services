package com.serbekun.ss.service.id;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Ulid}.
 * <p>
 * The encoder is hand-rolled, so the two extreme values are pinned against the
 * spec and everything else is decoded back with an independent decoder written
 * here — a shared bug between an encoder and its own inverse would otherwise go
 * unseen. The ordering tests are the point of the class: a ULID that does not
 * sort by time is just a slower UUID.
 */
class UlidTest {

    private static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

    // region encoding

    @Test
    void theTwoExtremeValuesMatchTheSpec() {
        assertThat(Ulid.encode(new byte[16])).isEqualTo("00000000000000000000000000");

        byte[] max = new byte[16];
        java.util.Arrays.fill(max, (byte) 0xFF);
        // 26 characters hold 130 bits, so the first one carries only three:
        // the largest ULID starts at '7', not 'Z'.
        assertThat(Ulid.encode(max)).isEqualTo("7ZZZZZZZZZZZZZZZZZZZZZZZZZ");
    }

    @Test
    void encodingIsAlwaysTwentySixCharactersOfTheAlphabet() {
        Ulid ulid = new Ulid();

        for (int i = 0; i < 200; i++) {
            String encoded = Ulid.encode(ulid.next());
            assertThat(encoded).hasSize(26);
            for (char c : encoded.toCharArray()) {
                assertThat(ALPHABET.indexOf(c)).as("'%s' is in the Crockford alphabet", c).isNotNegative();
            }
        }
    }

    @Test
    void everyBitSurvivesTheRoundTrip() {
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < 200; i++) {
            byte[] value = new byte[16];
            random.nextBytes(value);
            assertThat(decode(Ulid.encode(value))).as("round trip %s", i).isEqualTo(value);
        }
    }

    @Test
    void theTimestampIsReadableOutOfTheLeadingBits() {
        long now = System.currentTimeMillis();
        Ulid ulid = new Ulid(new SecureRandom(), () -> now);

        byte[] value = ulid.next();

        assertThat(Ulid.timestampOf(value)).isEqualTo(now);
        assertThat(Ulid.timestampOf(decode(Ulid.encode(value)))).isEqualTo(now);
        assertThat(Ulid.randomnessOf(value)).hasSize(10);
    }

    // endregion

    // region ordering

    @Test
    void idsMadeInTheSameMillisecondStillAscend() {
        // One frozen millisecond: only the monotonic counter can separate these.
        Ulid ulid = new Ulid(new SecureRandom(), () -> 1_700_000_000_000L);

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(Ulid.encode(ulid.next()));
        }

        assertThat(ids).isSorted();
        assertThat(ids).doesNotHaveDuplicates();
        // Every one of them still carries the millisecond it was made in.
        assertThat(ids).allMatch(id -> Ulid.timestampOf(decode(id)) == 1_700_000_000_000L);
    }

    @Test
    void aClockThatStepsBackwardsCannotBreakTheOrder() {
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        Ulid ulid = new Ulid(new SecureRandom(), clock::get);

        List<String> ids = new ArrayList<>();
        ids.add(Ulid.encode(ulid.next()));

        // An NTP correction drags the clock back a full second.
        clock.set(1_699_999_999_000L);
        for (int i = 0; i < 50; i++) {
            ids.add(Ulid.encode(ulid.next()));
        }

        // The ids keep ascending, and keep the timestamp already handed out
        // rather than one that would sort before it.
        assertThat(ids).isSorted();
        assertThat(ids).doesNotHaveDuplicates();
        assertThat(Ulid.timestampOf(decode(ids.get(ids.size() - 1)))).isEqualTo(1_700_000_000_000L);
    }

    @Test
    void idsAscendAcrossMilliseconds() {
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        Ulid ulid = new Ulid(new SecureRandom(), clock::getAndIncrement);

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ids.add(Ulid.encode(ulid.next()));
        }

        assertThat(ids).isSorted();
    }

    // endregion

    /** An independent Crockford base32 decoder, so the encoder is not its own witness. */
    private static byte[] decode(String encoded) {
        java.math.BigInteger value = java.math.BigInteger.ZERO;
        for (char c : encoded.toCharArray()) {
            value = value.shiftLeft(5).add(java.math.BigInteger.valueOf(ALPHABET.indexOf(c)));
        }

        byte[] out = new byte[16];
        byte[] magnitude = value.toByteArray();
        // BigInteger may prepend a sign byte or drop leading zeros; align to the low 16 bytes.
        int copy = Math.min(16, magnitude.length);
        System.arraycopy(magnitude, magnitude.length - copy, out, 16 - copy, copy);
        return out;
    }
}
