package com.serbekun.ss.service.hash;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.github.rctcwyvrn.blake3.Blake3;

/**
 * Hashing and integrity checking for SHA-256, SHA-512, BLAKE3 and HMAC-SHA256.
 * <p>
 * Everything here is stateless and streams where it can: a file is read in
 * chunks rather than held in memory. Digests are returned as lowercase hex,
 * and comparisons go through a constant-time check so a caller cannot learn a
 * secret HMAC by timing repeated guesses.
 */
public class HashService {

    /** Read buffer for streaming a file through a digest. */
    private static final int BUFFER_SIZE = 64 * 1024;

    /**
     * The outcome of hashing: the digest plus how many bytes went into it.
     *
     * @param algorithm the algorithm actually used
     * @param hash the digest as lowercase hex
     * @param bytes the number of bytes hashed
     */
    public record HashResult(HashAlgorithm algorithm, String hash, long bytes) {
    }

    /**
     * Hashes an in-memory payload.
     *
     * @param data the bytes to hash
     * @param algorithm the algorithm to use
     * @param key the HMAC secret; required for keyed algorithms, ignored otherwise
     * @return the digest and the byte count
     * @throws IllegalArgumentException when a keyed algorithm is used without a key
     */
    public HashResult hash(byte[] data, HashAlgorithm algorithm, byte[] key) {
        requireKeyConsistency(algorithm, key);

        String hex = switch (algorithm) {
            case SHA256 -> hexDigest(messageDigest("SHA-256"), data);
            case SHA512 -> hexDigest(messageDigest("SHA-512"), data);
            case BLAKE3 -> blake3(data);
            case HMAC_SHA256 -> HexFormat.of().formatHex(mac(key).doFinal(data));
        };

        return new HashResult(algorithm, hex, data.length);
    }

    /**
     * Hashes a stream without holding it in memory. The caller owns the stream
     * and is responsible for closing it.
     *
     * @param input the bytes to hash
     * @param algorithm the algorithm to use
     * @param key the HMAC secret; required for keyed algorithms, ignored otherwise
     * @return the digest and the byte count
     * @throws IOException if the stream cannot be read
     * @throws IllegalArgumentException when a keyed algorithm is used without a key
     */
    public HashResult hash(InputStream input, HashAlgorithm algorithm, byte[] key) throws IOException {
        requireKeyConsistency(algorithm, key);

        return switch (algorithm) {
            case SHA256 -> streamDigest(input, messageDigest("SHA-256"), algorithm);
            case SHA512 -> streamDigest(input, messageDigest("SHA-512"), algorithm);
            case BLAKE3 -> streamBlake3(input, algorithm);
            case HMAC_SHA256 -> streamMac(input, mac(key), algorithm);
        };
    }

    /**
     * Recomputes the digest of {@code data} and compares it with the one the
     * caller expects. The comparison is constant-time and ignores case and
     * surrounding whitespace in the expected value.
     *
     * @param data the bytes to hash
     * @param algorithm the algorithm to use
     * @param key the HMAC secret; required for keyed algorithms, ignored otherwise
     * @param expectedHash the digest the caller expects, as hex
     * @return the recomputed result and whether it matched
     * @throws IllegalArgumentException when the expected hash is blank or not hex
     */
    public VerifyResult verify(byte[] data, HashAlgorithm algorithm, byte[] key, String expectedHash) {
        if (expectedHash == null || expectedHash.isBlank()) {
            throw new IllegalArgumentException("hash is required");
        }

        String expected = expectedHash.strip().toLowerCase(java.util.Locale.ROOT);
        if (!expected.matches("[0-9a-f]+")) {
            throw new IllegalArgumentException("hash must be hex");
        }

        HashResult actual = hash(data, algorithm, key);
        return new VerifyResult(constantTimeEquals(expected, actual.hash()), expected, actual);
    }

    /**
     * The outcome of an integrity check.
     *
     * @param valid whether the recomputed digest matched the expected one
     * @param expected the digest the caller supplied, normalised to lowercase hex
     * @param actual the digest recomputed from the data
     */
    public record VerifyResult(boolean valid, String expected, HashResult actual) {
    }

    // region internals

    private static void requireKeyConsistency(HashAlgorithm algorithm, byte[] key) {
        if (algorithm.isKeyed() && (key == null || key.length == 0)) {
            throw new IllegalArgumentException("key is required for " + algorithm.wireName());
        }
    }

    private static MessageDigest messageDigest(String jcaName) {
        try {
            return MessageDigest.getInstance(jcaName);
        } catch (NoSuchAlgorithmException e) {
            // Both SHA-256 and SHA-512 are required of every Java platform.
            throw new IllegalStateException(jcaName + " is unavailable", e);
        }
    }

    private static Mac mac(byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        } catch (java.security.InvalidKeyException e) {
            throw new IllegalArgumentException("key is not usable for HMAC", e);
        }
    }

    private static String hexDigest(MessageDigest digest, byte[] data) {
        return HexFormat.of().formatHex(digest.digest(data));
    }

    private static String blake3(byte[] data) {
        Blake3 hasher = Blake3.newInstance();
        hasher.update(data);
        return hasher.hexdigest();
    }

    private static HashResult streamDigest(InputStream input, MessageDigest digest, HashAlgorithm algorithm)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            total += read;
        }
        return new HashResult(algorithm, HexFormat.of().formatHex(digest.digest()), total);
    }

    private static HashResult streamMac(InputStream input, Mac mac, HashAlgorithm algorithm) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            mac.update(buffer, 0, read);
            total += read;
        }
        return new HashResult(algorithm, HexFormat.of().formatHex(mac.doFinal()), total);
    }

    private static HashResult streamBlake3(InputStream input, HashAlgorithm algorithm) throws IOException {
        Blake3 hasher = Blake3.newInstance();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            // This BLAKE3 implementation takes whole arrays only, so a short
            // final read has to be trimmed before it is fed in.
            hasher.update(read == buffer.length ? buffer : Arrays.copyOf(buffer, read));
            total += read;
        }
        return new HashResult(algorithm, hasher.hexdigest(), total);
    }

    /**
     * Compares two hex digests without leaking, through timing, how far along
     * they first differ.
     */
    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
            actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII)
        );
    }

    // endregion
}
