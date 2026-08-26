package com.serbekun.ss.service.id;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.function.LongSupplier;

/**
 * A monotonic ULID generator.
 * <p>
 * A ULID is 128 bits — 48 of millisecond timestamp followed by 80 of
 * randomness — written as 26 characters of Crockford base32, which sorts
 * lexicographically in the same order as it sorts by time. That last property
 * is the whole reason to prefer one over a UUID v4, and it only holds within a
 * millisecond if ids made in the same millisecond keep ascending: so rather
 * than drawing fresh randomness, this increments the previous value, as the
 * spec's monotonic option describes.
 * <p>
 * The same rule covers a clock that steps backwards — the last timestamp is
 * kept and incremented instead, so a leap-second correction or an NTP nudge
 * cannot produce an id that sorts before one already handed out.
 */
public class Ulid {

    /** Crockford base32 — no I, L, O or U, so a ULID cannot be misread aloud. */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** Bytes of randomness after the 48-bit timestamp. */
    private static final int RANDOM_BYTES = 10;

    /** Characters in the encoded form: 128 bits at 5 bits each, rounded up. */
    private static final int LENGTH = 26;

    private final SecureRandom random;

    /** The clock, injectable so the backwards-clock rule can be tested at all. */
    private final LongSupplier clock;

    /** The millisecond the last id was issued in. */
    private long lastTimestamp = -1;

    /** The randomness of the last id, as an 80-bit big-endian integer. */
    private final byte[] lastRandomness = new byte[RANDOM_BYTES];

    public Ulid() {
        this(new SecureRandom());
    }

    /** @param random the source of randomness; injectable so a test can pin it */
    public Ulid(SecureRandom random) {
        this(random, System::currentTimeMillis);
    }

    /**
     * @param random the source of randomness
     * @param clock the millisecond clock to stamp ids with
     */
    public Ulid(SecureRandom random, LongSupplier clock) {
        this.random = random;
        this.clock = clock;
    }

    /**
     * Issues the next ULID.
     *
     * @return the 128 bits, timestamp first
     */
    public synchronized byte[] next() {
        long now = clock.getAsLong();

        if (now > lastTimestamp) {
            lastTimestamp = now;
            random.nextBytes(lastRandomness);
        } else {
            // Same millisecond, or a clock that went backwards: keep the
            // timestamp and step the randomness so the order still holds.
            if (!increment(lastRandomness)) {
                // 2^80 ids inside one millisecond is not a thing that happens,
                // but if it did, borrowing the next millisecond is the only
                // way to stay ordered.
                lastTimestamp++;
                random.nextBytes(lastRandomness);
            }
        }

        byte[] value = new byte[16];
        for (int i = 0; i < 6; i++) {
            value[i] = (byte) (lastTimestamp >>> (40 - i * 8));
        }
        System.arraycopy(lastRandomness, 0, value, 6, RANDOM_BYTES);
        return value;
    }

    /**
     * Adds one to a big-endian integer in place.
     *
     * @return false when it wrapped around, i.e. every byte was 0xFF
     */
    private static boolean increment(byte[] value) {
        for (int i = value.length - 1; i >= 0; i--) {
            if (value[i] != (byte) 0xFF) {
                value[i]++;
                return true;
            }
            value[i] = 0;
        }
        return false;
    }

    /**
     * Writes 128 bits as 26 Crockford base32 characters.
     * <p>
     * 26 characters hold 130 bits, so the value is left-padded by two: the
     * first character only ever carries three bits and never exceeds '7'.
     *
     * @param value the 16 bytes to write
     * @return the encoded text
     */
    public static String encode(byte[] value) {
        // One byte of leading zeros supplies the two padding bits, and one
        // trailing byte keeps the 16-bit read window in bounds on the last
        // character; neither ever contributes a bit to the output.
        byte[] padded = new byte[18];
        System.arraycopy(value, 0, padded, 1, 16);

        char[] out = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            int bitOffset = 6 + i * 5;
            int byteIndex = bitOffset >>> 3;
            int shift = 11 - (bitOffset & 7);

            int window = ((padded[byteIndex] & 0xFF) << 8) | (padded[byteIndex + 1] & 0xFF);
            out[i] = ALPHABET[(window >>> shift) & 0x1F];
        }
        return new String(out);
    }

    /** The millisecond timestamp carried in the leading 48 bits. */
    public static long timestampOf(byte[] value) {
        long timestamp = 0;
        for (int i = 0; i < 6; i++) {
            timestamp = (timestamp << 8) | (value[i] & 0xFF);
        }
        return timestamp;
    }

    /** The randomness that follows the timestamp. */
    public static byte[] randomnessOf(byte[] value) {
        return Arrays.copyOfRange(value, 6, 16);
    }
}
