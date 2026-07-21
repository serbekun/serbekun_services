package com.serbekun.ss.service.cipher;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RsaServiceTest {

    private static String b64(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    private static String fromB64(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    @Test
    void generateRsaKeyPairReturnsBase64EncodedKeys() throws Exception {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();

        assertThat(keyPair.publicKey()).isNotBlank();
        assertThat(keyPair.privateKey()).isNotBlank();

        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
            new X509EncodedKeySpec(Base64.getDecoder().decode(keyPair.publicKey()))
        );
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
            new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keyPair.privateKey()))
        );

        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void generateRsaKeyPairReturnsDifferentKeys() {
        RsaService.RsaKeyPair first = RsaService.generateRsaKeyPair();
        RsaService.RsaKeyPair second = RsaService.generateRsaKeyPair();

        assertThat(first.publicKey()).isNotEqualTo(second.publicKey());
        assertThat(first.privateKey()).isNotEqualTo(second.privateKey());
    }

    @Test
    void encryptDecryptRoundtrip() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        String data = b64("Hello, привет, 日本語!");

        String encrypted = RsaService.encrypt(data, keyPair.publicKey());
        String decrypted = RsaService.decrypt(encrypted, keyPair.privateKey());

        assertThat(decrypted).isEqualTo(data);
        assertThat(fromB64(decrypted)).isEqualTo("Hello, привет, 日本語!");
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        String data = b64("same data");

        assertThat(RsaService.encrypt(data, keyPair.publicKey()))
            .isNotEqualTo(RsaService.encrypt(data, keyPair.publicKey()));
    }

    @Test
    void decryptWithWrongPrivateKeyFails() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        RsaService.RsaKeyPair otherKeyPair = RsaService.generateRsaKeyPair();
        String encrypted = RsaService.encrypt(b64("secret"), keyPair.publicKey());

        assertThatThrownBy(() -> RsaService.decrypt(encrypted, otherKeyPair.privateKey()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void tamperedCiphertextFails() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        byte[] encrypted = Base64.getDecoder().decode(RsaService.encrypt(b64("secret"), keyPair.publicKey()));
        encrypted[encrypted.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(encrypted);

        assertThatThrownBy(() -> RsaService.decrypt(tampered, keyPair.privateKey()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void invalidBase64InputThrowsIllegalArgument() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();

        assertThatThrownBy(() -> RsaService.encrypt("not base64 !!!", keyPair.publicKey()))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RsaService.decrypt("not base64 !!!", keyPair.privateKey()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oversizedPlaintextFails() {
        RsaService.RsaKeyPair keyPair = RsaService.generateRsaKeyPair();
        String tooLargeFor2048BitOaepSha256 = Base64.getEncoder().encodeToString(new byte[191]);

        assertThatThrownBy(() -> RsaService.encrypt(tooLargeFor2048BitOaepSha256, keyPair.publicKey()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void cipherServiceDelegatesToRsaService() {
        CipherService cipherService = new CipherService();

        RsaService.RsaKeyPair keyPair = cipherService.generateRsaKeyPair();
        String data = b64("via CipherService RSA");
        String decrypted = cipherService.decryptRsa(cipherService.encryptRsa(data, keyPair.publicKey()), keyPair.privateKey());

        assertThat(decrypted).isEqualTo(data);
    }
}
