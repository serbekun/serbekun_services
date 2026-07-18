package com.serbekun.ss.service.cipher;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesServiceTest {

    private static String b64(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String fromB64(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    @Test
    void generateAesKeyReturns256BitBase64Key() {
        String key = AesService.generateAesKey();

        assertThat(key).isNotBlank();
        byte[] keyBytes = Base64.getDecoder().decode(key);
        assertThat(keyBytes).hasSize(32); // 256 bit
    }

    @Test
    void generateAesKeyReturnsDifferentKeys() {
        assertThat(AesService.generateAesKey()).isNotEqualTo(AesService.generateAesKey());
    }

    @Test
    void encryptDecryptRoundtrip() {
        String key = AesService.generateAesKey();
        String data = b64("Hello, привет, 日本語!");

        String encrypted = AesService.encrypt(data, key);
        String decrypted = AesService.decrypt(encrypted, key);

        assertThat(decrypted).isEqualTo(data);
        assertThat(fromB64(decrypted)).isEqualTo("Hello, привет, 日本語!");
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        // GCM uses a fresh random IV per call
        String key = AesService.generateAesKey();
        String data = b64("same data");

        assertThat(AesService.encrypt(data, key)).isNotEqualTo(AesService.encrypt(data, key));
    }

    @Test
    void decryptWithWrongKeyFails() {
        String key = AesService.generateAesKey();
        String otherKey = AesService.generateAesKey();
        String encrypted = AesService.encrypt(b64("secret"), key);

        assertThatThrownBy(() -> AesService.decrypt(encrypted, otherKey))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void decryptTamperedDataFails() {
        String key = AesService.generateAesKey();
        byte[] encrypted = Base64.getDecoder().decode(AesService.encrypt(b64("secret"), key));
        encrypted[encrypted.length - 1] ^= 0x01; // flip a bit in the auth tag
        String tampered = Base64.getEncoder().encodeToString(encrypted);

        assertThatThrownBy(() -> AesService.decrypt(tampered, key))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void decryptTooShortDataThrowsIllegalArgument() {
        String key = AesService.generateAesKey();
        String tooShort = Base64.getEncoder().encodeToString(new byte[5]); // < 12-byte IV

        assertThatThrownBy(() -> AesService.decrypt(tooShort, key))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("too short");
    }

    @Test
    void invalidBase64InputThrowsIllegalArgument() {
        String key = AesService.generateAesKey();

        assertThatThrownBy(() -> AesService.encrypt("not base64 !!!", key))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AesService.decrypt("not base64 !!!", key))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cipherServiceDelegatesToAesService() {
        CipherService cipherService = new CipherService();

        String key = cipherService.generateAesKey();
        String data = b64("via CipherService");
        String decrypted = cipherService.decrypt(cipherService.encrypt(data, key), key);

        assertThat(decrypted).isEqualTo(data);
    }
}
