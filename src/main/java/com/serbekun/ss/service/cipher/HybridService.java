package com.serbekun.ss.service.cipher;

/**
 * Hybrid encryption: RSA on its own can only encrypt a payload smaller than
 * the key, so the actual data goes through AES-GCM with a fresh single-use key
 * and only that AES key is wrapped with RSA-OAEP. The result is a pair —
 * the wrapped key and the cipher text — and both are needed to decrypt.
 */
public class HybridService {

    /**
     * Output of a hybrid encryption: the RSA-wrapped AES key and the
     * AES-GCM cipher text, both Base64-encoded.
     *
     * @param encryptedKey the single-use AES key encrypted with the RSA public key
     * @param data the cipher text produced with that AES key
     */
    public record HybridPayload(String encryptedKey, String data) {
    }

    /**
     * Encrypts data of any size for the holder of the matching RSA private key.
     *
     * @param dataBase64 the plain data in Base64
     * @param publicKeyBase64 the RSA public key in Base64
     * @return the wrapped AES key together with the cipher text
     */
    public static HybridPayload encrypt(String dataBase64, String publicKeyBase64) {
        String aesKeyBase64 = AesService.generateAesKey();

        // The AES key is already Base64 of its raw bytes, which is exactly what
        // RsaService.encrypt expects as its payload.
        String encryptedKey = RsaService.encrypt(aesKeyBase64, publicKeyBase64);
        String encryptedData = AesService.encrypt(dataBase64, aesKeyBase64);

        return new HybridPayload(encryptedKey, encryptedData);
    }

    /**
     * Unwraps the AES key with the RSA private key and decrypts the cipher text.
     *
     * @param encryptedDataBase64 the cipher text in Base64
     * @param encryptedKeyBase64 the RSA-wrapped AES key in Base64
     * @param privateKeyBase64 the RSA private key in Base64
     * @return the decrypted data in Base64
     */
    public static String decrypt(String encryptedDataBase64, String encryptedKeyBase64, String privateKeyBase64) {
        String aesKeyBase64 = RsaService.decrypt(encryptedKeyBase64, privateKeyBase64);
        return AesService.decrypt(encryptedDataBase64, aesKeyBase64);
    }
}
