package com.serbekun.ss.service.cipher;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridServiceTest {

    private static String b64(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String fromB64(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    @Test
    void encryptDecryptRoundtrip() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        String data = b64("Hello, привет, 日本語!");

        HybridService.HybridPayload payload = HybridService.encrypt(data, keyPair.publicKey());
        String decrypted = HybridService.decrypt(payload.data(), payload.encryptedKey(), keyPair.privateKey());

        assertThat(payload.encryptedKey()).isNotBlank();
        assertThat(payload.data()).isNotBlank();
        assertThat(decrypted).isEqualTo(data);
        assertThat(fromB64(decrypted)).isEqualTo("Hello, привет, 日本語!");
    }

    @Test
    void handlesPayloadsFarLargerThanTheRsaKey() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        byte[] large = new byte[512 * 1024];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        String data = Base64.getEncoder().encodeToString(large);

        HybridService.HybridPayload payload = HybridService.encrypt(data, keyPair.publicKey());

        assertThat(HybridService.decrypt(payload.data(), payload.encryptedKey(), keyPair.privateKey()))
            .isEqualTo(data);
    }

    @Test
    void everyCallUsesAFreshAesKey() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        String data = b64("same data");

        HybridService.HybridPayload first = HybridService.encrypt(data, keyPair.publicKey());
        HybridService.HybridPayload second = HybridService.encrypt(data, keyPair.publicKey());

        assertThat(first.encryptedKey()).isNotEqualTo(second.encryptedKey());
        assertThat(first.data()).isNotEqualTo(second.data());
    }

    @Test
    void decryptWithWrongPrivateKeyFails() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        RsaService.RsaKeyPair otherKeyPair = RsaService.generateRsaKeyPair();
        HybridService.HybridPayload payload = HybridService.encrypt(b64("secret"), keyPair.publicKey());

        assertThatThrownBy(() ->
            HybridService.decrypt(payload.data(), payload.encryptedKey(), otherKeyPair.privateKey()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void tamperedCipherTextFailsTheGcmTag() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        HybridService.HybridPayload payload = HybridService.encrypt(b64("secret"), keyPair.publicKey());

        byte[] cipherText = Base64.getDecoder().decode(payload.data());
        cipherText[cipherText.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(cipherText);

        assertThatThrownBy(() ->
            HybridService.decrypt(tampered, payload.encryptedKey(), keyPair.privateKey()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void invalidBase64InputThrowsIllegalArgument() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();

        assertThatThrownBy(() -> HybridService.encrypt("not base64 !!!", keyPair.publicKey()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cipherServiceDelegatesToHybridService() {
        CipherService cipherService = new CipherService();

        RsaService.RsaKeyPair keyPair = cipherService.generateRsaKeyPair();
        String data = b64("via CipherService hybrid");

        HybridService.HybridPayload payload = cipherService.encryptHybrid(data, keyPair.publicKey());

        assertThat(cipherService.decryptHybrid(payload.data(), payload.encryptedKey(), keyPair.privateKey()))
            .isEqualTo(data);
    }
}
