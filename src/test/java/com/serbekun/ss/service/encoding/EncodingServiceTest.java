package com.serbekun.ss.service.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EncodingService} and the formats behind it.
 * <p>
 * The RFC 4648 test vectors are pinned first, because every other test in here
 * only proves the code is self-consistent — these are the ones that prove it
 * agrees with the rest of the world. Base32 is hand-rolled (the JDK has none),
 * so its vectors are the load-bearing ones.
 */
class EncodingServiceTest {

    /** The RFC 4648 §10 inputs, in order. */
    private static final String[] VECTOR_INPUTS = {"", "f", "fo", "foo", "foob", "fooba", "foobar"};

    private final EncodingService service = new EncodingService();

    private String convert(String data, EncodingFormat from, EncodingFormat to) {
        return service.convert(data, from, to).data();
    }

    // region RFC 4648 vectors

    @Test
    void base64MatchesTheRfcVectors() {
        String[] expected = {"", "Zg==", "Zm8=", "Zm9v", "Zm9vYg==", "Zm9vYmE=", "Zm9vYmFy"};

        for (int i = 0; i < VECTOR_INPUTS.length; i++) {
            assertThat(convert(VECTOR_INPUTS[i], EncodingFormat.UTF8, EncodingFormat.BASE64))
                .as("base64 of '%s'", VECTOR_INPUTS[i])
                .isEqualTo(expected[i]);
            assertThat(convert(expected[i], EncodingFormat.BASE64, EncodingFormat.UTF8))
                .isEqualTo(VECTOR_INPUTS[i]);
        }
    }

    @Test
    void base32MatchesTheRfcVectors() {
        String[] expected = {
            "", "MY======", "MZXQ====", "MZXW6===", "MZXW6YQ=", "MZXW6YTB", "MZXW6YTBOI======"};

        for (int i = 0; i < VECTOR_INPUTS.length; i++) {
            assertThat(convert(VECTOR_INPUTS[i], EncodingFormat.UTF8, EncodingFormat.BASE32))
                .as("base32 of '%s'", VECTOR_INPUTS[i])
                .isEqualTo(expected[i]);
            assertThat(convert(expected[i], EncodingFormat.BASE32, EncodingFormat.UTF8))
                .isEqualTo(VECTOR_INPUTS[i]);
        }
    }

    @Test
    void hexMatchesTheRfcVectors() {
        String[] expected = {"", "66", "666f", "666f6f", "666f6f62", "666f6f6261", "666f6f626172"};

        for (int i = 0; i < VECTOR_INPUTS.length; i++) {
            assertThat(convert(VECTOR_INPUTS[i], EncodingFormat.UTF8, EncodingFormat.HEX))
                .as("hex of '%s'", VECTOR_INPUTS[i])
                .isEqualTo(expected[i]);
        }
    }

    @Test
    void base64UrlUsesItsOwnAlphabetAndNoPadding() {
        // 0xFB 0xFF encodes to "+/" in the standard alphabet.
        String bytes = "fbff";

        assertThat(convert(bytes, EncodingFormat.HEX, EncodingFormat.BASE64)).isEqualTo("+/8=");
        assertThat(convert(bytes, EncodingFormat.HEX, EncodingFormat.BASE64URL)).isEqualTo("-_8");
    }

    // endregion

    // region round trips

    @Test
    void everyFormatRoundTripsArbitraryBytes() {
        byte[] payload = new byte[512];
        new Random(20260826).nextBytes(payload);
        String hex = convert(bytesAsHex(payload), EncodingFormat.HEX, EncodingFormat.HEX);

        for (EncodingFormat format : EncodingFormat.values()) {
            if (format == EncodingFormat.UTF8) {
                continue; // random bytes are not text; that case is its own test
            }
            String encoded = convert(hex, EncodingFormat.HEX, format);
            assertThat(convert(encoded, format, EncodingFormat.HEX))
                .as("round trip through %s", format.wireName())
                .isEqualTo(hex);
        }
    }

    @Test
    void textRoundTripsThroughEveryFormat() {
        String text = "Привет, мир — 日本語 ✓ & a+b=c/d?e#f";

        for (EncodingFormat format : EncodingFormat.values()) {
            String encoded = convert(text, EncodingFormat.UTF8, format);
            assertThat(convert(encoded, format, EncodingFormat.UTF8))
                .as("round trip through %s", format.wireName())
                .isEqualTo(text);
        }
    }

    @Test
    void byteCountIsThePayloadNotTheEncodedText() {
        // Four characters of base64, three bytes of payload.
        EncodingService.ConvertResult result =
            service.convert("Zm9v", EncodingFormat.BASE64, EncodingFormat.HEX);

        assertThat(result.data()).isEqualTo("666f6f");
        assertThat(result.bytes()).isEqualTo(3);
        assertThat(result.from()).isEqualTo(EncodingFormat.BASE64);
        assertThat(result.to()).isEqualTo(EncodingFormat.HEX);
    }

    @Test
    void anEmptyPayloadIsValid() {
        assertThat(service.convert("", EncodingFormat.UTF8, EncodingFormat.BASE64).data()).isEmpty();
        assertThat(service.convert("", EncodingFormat.UTF8, EncodingFormat.BASE64).bytes()).isZero();
    }

    // endregion

    // region url encoding

    @Test
    void urlEncodingFollowsRfc3986() {
        assertThat(convert("a b+c", EncodingFormat.UTF8, EncodingFormat.URL)).isEqualTo("a%20b%2Bc");
        assertThat(convert("a%20b%2Bc", EncodingFormat.URL, EncodingFormat.UTF8)).isEqualTo("a b+c");

        // The unreserved set is left exactly as it is.
        assertThat(convert("aZ0-._~", EncodingFormat.UTF8, EncodingFormat.URL)).isEqualTo("aZ0-._~");
    }

    @Test
    void formEncodingWritesASpaceAsPlus() {
        assertThat(convert("a b+c", EncodingFormat.UTF8, EncodingFormat.URL_FORM)).isEqualTo("a+b%2Bc");
        assertThat(convert("a+b%2Bc", EncodingFormat.URL_FORM, EncodingFormat.UTF8)).isEqualTo("a b+c");

        // The whole point of keeping the two apart: the same text, read the
        // other way round, comes back different.
        assertThat(convert("a+b", EncodingFormat.URL, EncodingFormat.UTF8)).isEqualTo("a+b");
        assertThat(convert("a+b", EncodingFormat.URL_FORM, EncodingFormat.UTF8)).isEqualTo("a b");
    }

    @Test
    void urlEncodingIsUtf8PerByte() {
        assertThat(convert("é", EncodingFormat.UTF8, EncodingFormat.URL)).isEqualTo("%C3%A9");
        assertThat(convert("%c3%a9", EncodingFormat.URL, EncodingFormat.UTF8)).isEqualTo("é");
    }

    @Test
    void halfEncodedTextStillDecodes() {
        // Characters that were never escaped contribute their own bytes.
        assertThat(convert("привет%20мир", EncodingFormat.URL, EncodingFormat.UTF8)).isEqualTo("привет мир");
    }

    // endregion

    // region tolerant decoding

    @Test
    void base64DecodingForgivesWhitespaceAndTheUrlSafeAlphabet() {
        assertThat(convert("Zm9v\nYmFy\n", EncodingFormat.BASE64, EncodingFormat.UTF8)).isEqualTo("foobar");
        assertThat(convert("Zm9v YmFy", EncodingFormat.BASE64, EncodingFormat.UTF8)).isEqualTo("foobar");

        // A JWT segment means what it says even on the standard route.
        assertThat(convert("-_8", EncodingFormat.BASE64, EncodingFormat.HEX)).isEqualTo("fbff");
        assertThat(convert("+/8=", EncodingFormat.BASE64URL, EncodingFormat.HEX)).isEqualTo("fbff");
    }

    @Test
    void base32DecodingForgivesCaseAndMissingPadding() {
        assertThat(convert("mzxw6ytboi======", EncodingFormat.BASE32, EncodingFormat.UTF8)).isEqualTo("foobar");
        assertThat(convert("MZXW6YTBOI", EncodingFormat.BASE32, EncodingFormat.UTF8)).isEqualTo("foobar");
        assertThat(convert("MZXW 6YTB OI", EncodingFormat.BASE32, EncodingFormat.UTF8)).isEqualTo("foobar");
    }

    @Test
    void hexDecodingForgivesTheSeparatorsADumpCarries() {
        assertThat(convert("66:6F:6F", EncodingFormat.HEX, EncodingFormat.UTF8)).isEqualTo("foo");
        assertThat(convert("0x666F6F", EncodingFormat.HEX, EncodingFormat.UTF8)).isEqualTo("foo");
        assertThat(convert("66 6f 6f", EncodingFormat.HEX, EncodingFormat.UTF8)).isEqualTo("foo");
    }

    // endregion

    // region rejected input

    @Test
    void inputThatIsNotValidInItsFormatIsRejected() {
        assertThatThrownBy(() -> convert("not base64!!", EncodingFormat.BASE64, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid base64");

        // A full block, so the length check passes and the alphabet check is what fires.
        assertThatThrownBy(() -> convert("MZXW6YT1", EncodingFormat.BASE32, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not in the alphabet");

        // 9 characters is one over a block — no whole number of bytes ends there.
        assertThatThrownBy(() -> convert("MZXW6YTBO", EncodingFormat.BASE32, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ends mid-byte");

        assertThatThrownBy(() -> convert("666f6", EncodingFormat.HEX, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("odd number of digits");

        assertThatThrownBy(() -> convert("zz", EncodingFormat.HEX, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid hex");

        assertThatThrownBy(() -> convert("a%2", EncodingFormat.URL, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("two hex digits");

        assertThatThrownBy(() -> convert("a%zz", EncodingFormat.URL, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a hex escape");
    }

    @Test
    void bytesThatAreNotTextAreRefusedRatherThanMangled() {
        // 0x80 alone is not valid UTF-8; the lenient decoder would answer '�'.
        assertThatThrownBy(() -> convert("80", EncodingFormat.HEX, EncodingFormat.UTF8))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not valid UTF-8 text");
    }

    @Test
    void aMissingPayloadIsRejected() {
        assertThatThrownBy(() -> service.convert(null, EncodingFormat.UTF8, EncodingFormat.HEX))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("data is required");
    }

    // endregion

    // region format names

    @Test
    void formatNamesAreMatchedLoosely() {
        assertThat(EncodingFormat.fromWire("BASE-64", "from")).isEqualTo(EncodingFormat.BASE64);
        assertThat(EncodingFormat.fromWire("b64", "from")).isEqualTo(EncodingFormat.BASE64);
        assertThat(EncodingFormat.fromWire("Base32", "from")).isEqualTo(EncodingFormat.BASE32);
        assertThat(EncodingFormat.fromWire("base16", "from")).isEqualTo(EncodingFormat.HEX);
        assertThat(EncodingFormat.fromWire("text", "from")).isEqualTo(EncodingFormat.UTF8);
        assertThat(EncodingFormat.fromWire(" utf-8 ", "from")).isEqualTo(EncodingFormat.UTF8);
        assertThat(EncodingFormat.fromWire("percent", "from")).isEqualTo(EncodingFormat.URL);
        assertThat(EncodingFormat.fromWire("form", "from")).isEqualTo(EncodingFormat.URL_FORM);
    }

    @Test
    void anUnknownFormatNamesTheFieldItCameFrom() {
        assertThatThrownBy(() -> EncodingFormat.fromWire("rot13", "to"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported to 'rot13'")
            .hasMessageContaining("base64");

        assertThatThrownBy(() -> EncodingFormat.fromWire(null, "from"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("from is required");
    }

    // endregion

    private static String bytesAsHex(byte[] payload) {
        return java.util.HexFormat.of().formatHex(payload);
    }

    @Test
    void utf8IsTheBytesOfTheText() {
        assertThat("é".getBytes(StandardCharsets.UTF_8)).hasSize(2);
        assertThat(service.convert("é", EncodingFormat.UTF8, EncodingFormat.HEX).bytes()).isEqualTo(2);
    }
}
