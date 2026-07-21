package com.serbekun.ss.service.cipher;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

public class RsaService {

    private static final String KEY_ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/OAEPPadding";
    private static final int KEY_SIZE_BITS = 2048;
    private static final OAEPParameterSpec OAEP_SHA_256_SPEC = new OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT
    );

    public record RsaKeyPair(String publicKey, String privateKey) {
    }

    /**
     * Generates a 2048-bit RSA key pair.
     *
     * @return public and private keys as Base64-encoded DER values
     */
    public static RsaKeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGenerator.initialize(KEY_SIZE_BITS);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            return new RsaKeyPair(
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypts Base64-encoded data using a Base64-encoded X.509 RSA public key.
     *
     * @param dataBase64 the plain data in Base64
     * @param publicKeyBase64 the RSA public key in Base64
     * @return encrypted data in Base64
     */
    public static String encrypt(String dataBase64, String publicKeyBase64) {
        try {
            byte[] dataBytes = Base64.getDecoder().decode(dataBase64);
            PublicKey publicKey = readPublicKey(publicKeyBase64);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SHA_256_SPEC);
            return Base64.getEncoder().encodeToString(cipher.doFinal(dataBytes));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts Base64-encoded RSA ciphertext using a Base64-encoded PKCS#8 private key.
     *
     * @param encryptedDataBase64 the encrypted data in Base64
     * @param privateKeyBase64 the RSA private key in Base64
     * @return decrypted data in Base64
     */
    public static String decrypt(String encryptedDataBase64, String privateKeyBase64) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedDataBase64);
            PrivateKey privateKey = readPrivateKey(privateKeyBase64);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SHA_256_SPEC);
            return Base64.getEncoder().encodeToString(cipher.doFinal(encryptedBytes));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PublicKey readPublicKey(String publicKeyBase64) throws Exception {
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKeyBytes));
    }

    private static PrivateKey readPrivateKey(String privateKeyBase64) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
    }
}
