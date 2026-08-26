package com.serbekun.ss.service.id;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.serbekun.ss.service.encoding.EncodingFormat;

/**
 * Identifiers and random material: UUIDs, ULIDs, tokens and raw bytes.
 * <p>
 * Everything here comes from one {@link SecureRandom}. That is not decoration —
 * these values are used as session tokens and delete keys, and a predictable
 * generator would make every one of them guessable. Nothing is stored: an id is
 * made, handed back and forgotten, so the same request never returns the same
 * answer twice.
 * <p>
 * Every request is a {@link Spec} and every answer a {@link Result}, which is
 * what lets one batch call mix kinds freely — the batch endpoint is a list of
 * exactly the specs the single-purpose endpoints build for themselves.
 */
public class IdService {

    /** Most values one request may ask for. */
    public static final int MAX_COUNT = 1000;

    /** Values returned when the caller does not ask for a number. */
    public static final int DEFAULT_COUNT = 1;

    /** Longest token accepted, in characters. */
    public static final int MAX_TOKEN_LENGTH = 4096;

    /** Token length when the caller does not ask for one: 32 base62 characters is ~190 bits. */
    public static final int DEFAULT_TOKEN_LENGTH = 32;

    /** Most random bytes one value may hold. */
    public static final int MAX_BYTES_LENGTH = 4096;

    /** Byte count when the caller does not ask for one. */
    public static final int DEFAULT_BYTES_LENGTH = 32;

    /** Most characters a custom alphabet may hold. */
    private static final int MAX_ALPHABET_SIZE = 256;

    /**
     * One request, in the words the caller used.
     * <p>
     * The fields are raw wire values rather than resolved enums so that
     * validation — and the error message that comes with it — lives here in the
     * service, whichever endpoint the request arrived through.
     *
     * @param type what to generate
     * @param count how many; null means {@value #DEFAULT_COUNT}
     * @param version for uuid: {@code v4} or {@code v7}; null means v4
     * @param format for uuid, ulid and bytes: how to write the value; null means each one's default
     * @param length for token: characters; for bytes: bytes. Null means that type's default
     * @param alphabet for token: a named alphabet; null means base62
     * @param chars for token: a set of characters to use instead of a named alphabet
     */
    public record Spec(
            IdType type,
            Integer count,
            String version,
            String format,
            Integer length,
            String alphabet,
            String chars) {

        /** The one-value request the {@code GET} endpoints start from. */
        public static Spec of(IdType type) {
            return new Spec(type, null, null, null, null, null, null);
        }
    }

    /**
     * One answer.
     *
     * @param type what was generated
     * @param count how many values came back
     * @param values the values themselves
     * @param format how they were written, where that was a choice
     * @param version the uuid version, for uuids only
     * @param alphabet the alphabet a token was drawn from, for tokens only
     * @param length the length asked for, for tokens and bytes only
     * @param bits how much randomness one value carries — the number that
     *             actually says whether a token is hard to guess
     */
    public record Result(
            String type,
            int count,
            List<String> values,
            String format,
            String version,
            String alphabet,
            Integer length,
            int bits) {
    }

    /** Random bits in a UUID v4: everything except the 4 version and 2 variant bits. */
    private static final int UUID_V4_BITS = 122;

    /** Random bits in a UUID v7: 12 of rand_a plus 62 of rand_b; the rest is the timestamp. */
    private static final int UUID_V7_BITS = 74;

    /** Random bits in a ULID: the 80 that follow the timestamp. */
    private static final int ULID_BITS = 80;

    private final SecureRandom random;
    private final Ulid ulid;

    public IdService() {
        this(new SecureRandom());
    }

    /** @param random the source of randomness; injectable so a test can pin it */
    public IdService(SecureRandom random) {
        this.random = random;
        this.ulid = new Ulid(random);
    }

    /**
     * Generates one batch of values.
     *
     * @param spec what to generate
     * @return the values and what they are
     * @throws IllegalArgumentException when any part of the spec is out of range
     *         or names something unsupported
     */
    public Result generate(Spec spec) {
        if (spec == null || spec.type() == null) {
            throw new IllegalArgumentException("type is required — supported: " + IdType.supported());
        }

        int count = count(spec.count());
        return switch (spec.type()) {
            case UUID -> uuids(spec, count);
            case ULID -> ulids(spec, count);
            case TOKEN -> tokens(spec, count);
            case BYTES -> bytes(spec, count);
        };
    }

    /**
     * Generates several batches in one call, so a client can fill a whole form
     * of ids without a request per field.
     *
     * @param specs what to generate, in order
     * @return one result per spec, in the same order
     * @throws IllegalArgumentException when the list is empty, or the values
     *         asked for across all of it exceed {@value #MAX_COUNT}
     */
    public List<Result> batch(List<Spec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("at least one item is required");
        }

        // The cap is on the whole call, not on each item — otherwise a hundred
        // items of a thousand values each would slip past a per-item limit.
        int total = 0;
        for (Spec spec : specs) {
            total += count(spec == null ? null : spec.count());
        }
        if (total > MAX_COUNT) {
            throw new IllegalArgumentException(
                "a batch may hold at most " + MAX_COUNT + " values in total, this one asks for " + total);
        }

        List<Result> results = new ArrayList<>(specs.size());
        for (Spec spec : specs) {
            results.add(generate(spec));
        }
        return results;
    }

    // region generators

    private Result uuids(Spec spec, int count) {
        UuidVersion version = UuidVersion.fromWire(spec.version());
        UuidFormat format = UuidFormat.fromWire(spec.format());

        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(format.format(version == UuidVersion.V4 ? uuidV4() : uuidV7()));
        }

        return new Result(IdType.UUID.wireName(), count, values, format.wireName(),
            version.wireName(), null, null, version == UuidVersion.V4 ? UUID_V4_BITS : UUID_V7_BITS);
    }

    /** A v4 from the platform generator, which is itself {@link SecureRandom}-backed. */
    private static UUID uuidV4() {
        return UUID.randomUUID();
    }

    /**
     * A v7: 48 bits of millisecond timestamp, then the version and variant
     * nibbles with random bits filling everything around them.
     */
    private UUID uuidV7() {
        byte[] randomness = new byte[10];
        random.nextBytes(randomness);

        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFFFFFL;
        long high = (timestamp << 16)
            | (0x7L << 12)                                   // version 7
            | (((long) (randomness[0] & 0x0F)) << 8)         // rand_a, 12 bits
            | (randomness[1] & 0xFFL);

        long low = 0x8000000000000000L;                      // variant 0b10
        low |= ((long) (randomness[2] & 0x3F)) << 56;        // rand_b, 62 bits
        for (int i = 3; i < 10; i++) {
            low |= ((long) (randomness[i] & 0xFF)) << (8 * (9 - i));
        }

        return new UUID(high, low);
    }

    private Result ulids(Spec spec, int count) {
        UlidFormat format = UlidFormat.fromWire(spec.format());

        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(format.format(ulid.next()));
        }

        return new Result(IdType.ULID.wireName(), count, values, format.wireName(),
            null, null, null, ULID_BITS);
    }

    private Result tokens(Spec spec, int count) {
        String alphabet = alphabet(spec);
        int length = length(spec.length(), DEFAULT_TOKEN_LENGTH, MAX_TOKEN_LENGTH, "length");

        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(token(alphabet, length));
        }

        String name = spec.chars() != null && !spec.chars().isBlank()
            ? "custom"
            : TokenAlphabet.fromWire(spec.alphabet()).wireName();

        return new Result(IdType.TOKEN.wireName(), count, values, null, null, name, length,
            entropyBits(alphabet.length(), length));
    }

    /**
     * Draws {@code length} characters from {@code alphabet}.
     * <p>
     * {@code nextInt(bound)} is used rather than {@code nextInt() % size}: the
     * modulo form quietly favours the first characters of any alphabet whose
     * size is not a power of two, which is most of them.
     */
    private String token(String alphabet, int length) {
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            out.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return out.toString();
    }

    private Result bytes(Spec spec, int count) {
        EncodingFormat format = bytesFormat(spec.format());
        int length = length(spec.length(), DEFAULT_BYTES_LENGTH, MAX_BYTES_LENGTH, "length");

        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte[] material = new byte[length];
            random.nextBytes(material);
            values.add(format.encode(material));
        }

        return new Result(IdType.BYTES.wireName(), count, values, format.wireName(),
            null, null, length, length * 8);
    }

    // endregion

    // region validation

    private static int count(Integer requested) {
        int count = requested == null ? DEFAULT_COUNT : requested;
        if (count < 1 || count > MAX_COUNT) {
            throw new IllegalArgumentException("count must be between 1 and " + MAX_COUNT);
        }
        return count;
    }

    private static int length(Integer requested, int fallback, int max, String field) {
        int length = requested == null ? fallback : requested;
        if (length < 1 || length > max) {
            throw new IllegalArgumentException(field + " must be between 1 and " + max);
        }
        return length;
    }

    /**
     * The characters a token is drawn from: the caller's own set if they gave
     * one, otherwise a named alphabet.
     *
     * @throws IllegalArgumentException when a custom set is too small, too
     *         large, or repeats a character
     */
    private static String alphabet(Spec spec) {
        String custom = spec.chars();
        if (custom == null || custom.isBlank()) {
            return TokenAlphabet.fromWire(spec.alphabet()).characters();
        }

        if (custom.length() < 2 || custom.length() > MAX_ALPHABET_SIZE) {
            throw new IllegalArgumentException(
                "chars must hold between 2 and " + MAX_ALPHABET_SIZE + " characters");
        }
        // A repeated character is drawn twice as often, which quietly costs
        // entropy the reported bit count would not know about.
        if (custom.chars().distinct().count() != custom.length()) {
            throw new IllegalArgumentException("chars must not repeat a character");
        }
        return custom;
    }

    /** Random bytes are not text, so the one format that cannot hold them is refused by name. */
    private static EncodingFormat bytesFormat(String name) {
        EncodingFormat format = name == null || name.isBlank()
            ? EncodingFormat.HEX
            : EncodingFormat.fromWire(name, "format");

        if (format == EncodingFormat.UTF8) {
            throw new IllegalArgumentException(
                "random bytes are not text — use hex, base64, base64url or base32");
        }
        return format;
    }

    /**
     * How many bits of randomness one value carries.
     * <p>
     * Rounded down, because rounding a security margin up is how a token ends
     * up weaker than the number next to it claims.
     */
    private static int entropyBits(int alphabetSize, int length) {
        return (int) Math.floor(length * (Math.log(alphabetSize) / Math.log(2)));
    }

    // endregion
}
