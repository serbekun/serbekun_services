package com.serbekun.ss.service.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashServiceTest {

    private final HashService service = new HashService();

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String hash(String data, HashAlgorithm algorithm) {
        return service.hash(utf8(data), algorithm, null).hash();
    }

    // region known-answer tests

    @Test
    void sha256MatchesKnownVectors() {
        assertThat(hash("", HashAlgorithm.SHA256))
            .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(hash("hello", HashAlgorithm.SHA256))
            .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
        assertThat(hash("abc", HashAlgorithm.SHA256))
            .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(hash("The quick brown fox jumps over the lazy dog", HashAlgorithm.SHA256))
            .isEqualTo("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592");
    }

    @Test
    void sha512MatchesKnownVectors() {
        assertThat(hash("hello", HashAlgorithm.SHA512)).isEqualTo(
            "9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca7"
                + "2323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043");
    }

    @Test
    void blake3MatchesKnownVectors() {
        assertThat(hash("", HashAlgorithm.BLAKE3))
            .isEqualTo("af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262");
        assertThat(hash("hello", HashAlgorithm.BLAKE3))
            .isEqualTo("ea8f163db38682925e4491c5e58d4bb3506ef8c14eb78a86e908c5624a67200f");
        assertThat(hash("abc", HashAlgorithm.BLAKE3))
            .isEqualTo("6437b3ac38465133ffb63b75273a8db548c558465d79db03fd359c6cd5bd9d85");
        assertThat(hash("The quick brown fox jumps over the lazy dog", HashAlgorithm.BLAKE3))
            .isEqualTo("2f1514181aadccd913abd94cfa592701a5686ab23f8df1dff1b74710febc6d4a");
    }

    @Test
    void hmacSha256MatchesKnownVectors() {
        byte[] key = utf8("secret");
        assertThat(service.hash(utf8(""), HashAlgorithm.HMAC_SHA256, key).hash())
            .isEqualTo("f9e66e179b6747ae54108f82f8ade8b3c25d76fd30afde6c395822c530196169");
        assertThat(service.hash(utf8("hello"), HashAlgorithm.HMAC_SHA256, key).hash())
            .isEqualTo("88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b");
        assertThat(service.hash(utf8("abc"), HashAlgorithm.HMAC_SHA256, key).hash())
            .isEqualTo("9946dad4e00e913fc8be8e5d3f7e110a4a9e832f83fb09c345285d78638d8a0e");
    }

    @Test
    void nonAsciiPayloadsHashTheirUtf8Bytes() {
        assertThat(hash("Привет, мир! 日本語", HashAlgorithm.SHA256))
            .isEqualTo("f5b104e1dae0fea3867c97b2a371eec1317dd3d4978d7a9f7606210bb92eae67");
        assertThat(hash("Привет, мир! 日本語", HashAlgorithm.BLAKE3))
            .isEqualTo("6b768d2da08657459c0058b31d727946907141ed28176817a806f861cd9fef87");
    }

    // endregion

    // region blake3 official vectors

    /**
     * The official BLAKE3 test vectors. The lengths straddle every internal
     * boundary — the 1024-byte chunk, its multiples, and the odd byte past each
     * — which is where tree hashing goes wrong if it is going to.
     */
    static Stream<Object[]> blake3OfficialVectors() {
        return Stream.of(
            vector(0, "af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262"),
            vector(1, "2d3adedff11b61f14c886e35afa036736dcd87a74d27b5c1510225d0f592e213"),
            vector(2, "7b7015bb92cf0b318037702a6cdd81dee41224f734684c2c122cd6359cb1ee63"),
            vector(3, "e1be4d7a8ab5560aa4199eea339849ba8e293d55ca0a81006726d184519e647f"),
            vector(4, "f30f5ab28fe047904037f77b6da4fea1e27241c5d132638d8bedce9d40494f32"),
            vector(5, "b40b44dfd97e7a84a996a91af8b85188c66c126940ba7aad2e7ae6b385402aa2"),
            vector(6, "06c4e8ffb6872fad96f9aaca5eee1553eb62aed0ad7198cef42e87f6a616c844"),
            vector(7, "3f8770f387faad08faa9d8414e9f449ac68e6ff0417f673f602a646a891419fe"),
            vector(8, "2351207d04fc16ade43ccab08600939c7c1fa70a5c0aaca76063d04c3228eaeb"),
            vector(63, "e9bc37a594daad83be9470df7f7b3798297c3d834ce80ba85d6e207627b7db7b"),
            vector(64, "4eed7141ea4a5cd4b788606bd23f46e212af9cacebacdc7d1f4c6dc7f2511b98"),
            vector(65, "de1e5fa0be70df6d2be8fffd0e99ceaa8eb6e8c93a63f2d8d1c30ecb6b263dee"),
            vector(127, "d81293fda863f008c09e92fc382a81f5a0b4a1251cba1634016a0f86a6bd640d"),
            vector(128, "f17e570564b26578c33bb7f44643f539624b05df1a76c81f30acd548c44b45ef"),
            vector(129, "683aaae9f3c5ba37eaaf072aed0f9e30bac0865137bae68b1fde4ca2aebdcb12"),
            vector(1023, "10108970eeda3eb932baac1428c7a2163b0e924c9a9e25b35bba72b28f70bd11"),
            vector(1024, "42214739f095a406f3fc83deb889744ac00df831c10daa55189b5d121c855af7"),
            vector(1025, "d00278ae47eb27b34faecf67b4fe263f82d5412916c1ffd97c8cb7fb814b8444"),
            vector(2048, "e776b6028c7cd22a4d0ba182a8bf62205d2ef576467e838ed6f2529b85fba24a"),
            vector(2049, "5f4d72f40d7a5f82b15ca2b2e44b1de3c2ef86c426c95c1af0b6879522563030"),
            vector(3072, "b98cb0ff3623be03326b373de6b9095218513e64f1ee2edd2525c7ad1e5cffd2"),
            vector(3073, "7124b49501012f81cc7f11ca069ec9226cecb8a2c850cfe644e327d22d3e1cd3"),
            vector(4096, "015094013f57a5277b59d8475c0501042c0b642e531b0a1c8f58d2163229e969"),
            vector(4097, "9b4052b38f1c5fc8b1f9ff7ac7b27cd242487b3d890d15c96a1c25b8aa0fb995"),
            vector(5120, "9cadc15fed8b5d854562b26a9536d9707cadeda9b143978f319ab34230535833"),
            vector(5121, "628bd2cb2004694adaab7bbd778a25df25c47b9d4155a55f8fbd79f2fe154cff"),
            vector(6144, "3e2e5b74e048f3add6d21faab3f83aa44d3b2278afb83b80b3c35164ebeca205"),
            vector(6145, "f1323a8631446cc50536a9f705ee5cb619424d46887f3c376c695b70e0f0507f"),
            vector(7168, "61da957ec2499a95d6b8023e2b0e604ec7f6b50e80a9678b89d2628e99ada77a"),
            vector(7169, "a003fc7a51754a9b3c7fae0367ab3d782dccf28855a03d435f8cfe74605e7817"),
            vector(8192, "aae792484c8efe4f19e2ca7d371d8c467ffb10748d8a5a1ae579948f718a2a63"),
            vector(8193, "bab6c09cb8ce8cf459261398d2e7aef35700bf488116ceb94a36d0f5f1b7bc3b"),
            vector(16384, "f875d6646de28985646f34ee13be9a576fd515f76b5b0a26bb324735041ddde4"),
            vector(31744, "62b6960e1a44bcc1eb1a611a8d6235b6b4b78f32e7abc4fb4c6cdcce94895c47")
        );
    }

    private static Object[] vector(int length, String expected) {
        return new Object[] { length, expected };
    }

    /** The official vector input: byte {@code i} is {@code i % 251}. */
    private static byte[] officialInput(int length) {
        byte[] input = new byte[length];
        for (int i = 0; i < length; i++) {
            input[i] = (byte) (i % 251);
        }
        return input;
    }

    @ParameterizedTest(name = "blake3 of {0} bytes")
    @MethodSource("blake3OfficialVectors")
    void blake3MatchesOfficialVectors(int length, String expected) {
        assertThat(service.hash(officialInput(length), HashAlgorithm.BLAKE3, null).hash())
            .isEqualTo(expected);
    }

    @ParameterizedTest(name = "streamed blake3 of {0} bytes")
    @MethodSource("blake3OfficialVectors")
    void streamedBlake3MatchesOfficialVectors(int length, String expected) throws IOException {
        // Streaming trims a short final read before feeding the hasher; these
        // lengths make sure that trimming never changes the digest.
        assertThat(service.hash(new ByteArrayInputStream(officialInput(length)), HashAlgorithm.BLAKE3, null).hash())
            .isEqualTo(expected);
    }

    // endregion

    // region streaming

    @ParameterizedTest(name = "streamed == one-shot for {0}")
    @MethodSource("allAlgorithms")
    void streamingMatchesOneShot(HashAlgorithm algorithm) throws IOException {
        byte[] key = algorithm.isKeyed() ? utf8("secret") : null;

        for (int length : List.of(0, 1, 100, 65535, 65536, 65537, 200_000)) {
            byte[] data = officialInput(length);
            assertThat(service.hash(new ByteArrayInputStream(data), algorithm, key).hash())
                .as("%s of %d bytes", algorithm.wireName(), length)
                .isEqualTo(service.hash(data, algorithm, key).hash());
        }
    }

    static Stream<HashAlgorithm> allAlgorithms() {
        return Stream.of(HashAlgorithm.values());
    }

    @Test
    void streamingReportsTheByteCount() throws IOException {
        HashService.HashResult result =
            service.hash(new ByteArrayInputStream(new byte[4096]), HashAlgorithm.SHA256, null);
        assertThat(result.bytes()).isEqualTo(4096);
        assertThat(result.algorithm()).isEqualTo(HashAlgorithm.SHA256);
    }

    // endregion

    // region verify

    @Test
    void verifyAcceptsAMatchingHash() {
        HashService.VerifyResult result = service.verify(
            utf8("hello"), HashAlgorithm.SHA256, null,
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");

        assertThat(result.valid()).isTrue();
        assertThat(result.actual().hash()).isEqualTo(result.expected());
    }

    @Test
    void verifyIsCaseAndWhitespaceInsensitive() {
        HashService.VerifyResult result = service.verify(
            utf8("hello"), HashAlgorithm.SHA256, null,
            "  2CF24DBA5FB0A30E26E83B2AC5B9E29E1B161E5C1FA7425E73043362938B9824  ");

        assertThat(result.valid()).isTrue();
    }

    @Test
    void verifyRejectsAlteredData() {
        HashService.VerifyResult result = service.verify(
            utf8("hello!"), HashAlgorithm.SHA256, null,
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");

        assertThat(result.valid()).isFalse();
        assertThat(result.actual().hash()).isNotEqualTo(result.expected());
    }

    @Test
    void verifyRejectsAHashOfTheWrongLength() {
        HashService.VerifyResult result =
            service.verify(utf8("hello"), HashAlgorithm.SHA256, null, "2cf24dba");

        assertThat(result.valid()).isFalse();
    }

    @Test
    void verifyRejectsNonHexAndBlankHashes() {
        assertThatThrownBy(() -> service.verify(utf8("hello"), HashAlgorithm.SHA256, null, "zzzz"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hex");
        assertThatThrownBy(() -> service.verify(utf8("hello"), HashAlgorithm.SHA256, null, "  "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyWorksForKeyedAlgorithms() {
        byte[] key = utf8("secret");
        HashService.VerifyResult result = service.verify(utf8("hello"), HashAlgorithm.HMAC_SHA256, key,
            "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b");

        assertThat(result.valid()).isTrue();

        HashService.VerifyResult wrongKey = service.verify(utf8("hello"), HashAlgorithm.HMAC_SHA256, utf8("other"),
            "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b");

        assertThat(wrongKey.valid()).isFalse();
    }

    // endregion

    // region keys and algorithm names

    @Test
    void keyedAlgorithmsRequireAKey() {
        assertThatThrownBy(() -> service.hash(utf8("hello"), HashAlgorithm.HMAC_SHA256, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("key is required");
        assertThatThrownBy(() -> service.hash(utf8("hello"), HashAlgorithm.HMAC_SHA256, new byte[0]))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unkeyedAlgorithmsIgnoreAKey() {
        assertThat(service.hash(utf8("hello"), HashAlgorithm.SHA256, utf8("ignored")).hash())
            .isEqualTo(hash("hello", HashAlgorithm.SHA256));
    }

    @Test
    void differentKeysProduceDifferentMacs() {
        assertThat(service.hash(utf8("hello"), HashAlgorithm.HMAC_SHA256, utf8("a")).hash())
            .isNotEqualTo(service.hash(utf8("hello"), HashAlgorithm.HMAC_SHA256, utf8("b")).hash());
    }

    @Test
    void algorithmNamesAreAcceptedInCommonSpellings() {
        assertThat(HashAlgorithm.fromWire("sha256")).isEqualTo(HashAlgorithm.SHA256);
        assertThat(HashAlgorithm.fromWire("SHA-256")).isEqualTo(HashAlgorithm.SHA256);
        assertThat(HashAlgorithm.fromWire("Sha_256")).isEqualTo(HashAlgorithm.SHA256);
        assertThat(HashAlgorithm.fromWire("sha512")).isEqualTo(HashAlgorithm.SHA512);
        assertThat(HashAlgorithm.fromWire("BLAKE3")).isEqualTo(HashAlgorithm.BLAKE3);
        assertThat(HashAlgorithm.fromWire("hmac-sha256")).isEqualTo(HashAlgorithm.HMAC_SHA256);
        assertThat(HashAlgorithm.fromWire("hmacsha256")).isEqualTo(HashAlgorithm.HMAC_SHA256);
        assertThat(HashAlgorithm.fromWire("HMAC_SHA256")).isEqualTo(HashAlgorithm.HMAC_SHA256);
    }

    @Test
    void unknownAlgorithmNamesAreRejectedWithTheSupportedList() {
        assertThatThrownBy(() -> HashAlgorithm.fromWire("md5"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("md5")
            .hasMessageContaining("sha256")
            .hasMessageContaining("blake3");
        assertThatThrownBy(() -> HashAlgorithm.fromWire(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HashAlgorithm.fromWire(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void wireNamesAreStable() {
        assertThat(HashAlgorithm.SHA256.wireName()).isEqualTo("sha256");
        assertThat(HashAlgorithm.SHA512.wireName()).isEqualTo("sha512");
        assertThat(HashAlgorithm.BLAKE3.wireName()).isEqualTo("blake3");
        assertThat(HashAlgorithm.HMAC_SHA256.wireName()).isEqualTo("hmac-sha256");
        assertThat(HashAlgorithm.supported()).isEqualTo("sha256, sha512, blake3, hmac-sha256");
    }

    // endregion
}
