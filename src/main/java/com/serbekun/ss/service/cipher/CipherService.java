package com.serbekun.ss.service.cipher;

public class CipherService {

    /**
     * Generates a new AES key for encryption and decryption.
     * @return the generated AES key as a Base64-encoded string.
     */
    public String generateAesKey() {
        return AesService.generateAesKey();
    }

    /**
     * Encrypts the provided data using the specified AES key.
     * @param dataBase64 the data to encrypt, as a Base64-encoded string.
     * @param keyBase64 the AES key, as a Base64-encoded string.
     * @return the encrypted data as a Base64-encoded string.
     */
    public String encrypt(String dataBase64, String keyBase64) {
        return AesService.encrypt(dataBase64, keyBase64);
    }

    /**
     * Decrypts the provided encrypted data using the specified AES key.
     * @param encryptedDataBase64 the encrypted data, as a Base64-encoded string.
     * @param keyBase64 the AES key, as a Base64-encoded string.
     * @return the decrypted data as a Base64-encoded string.
     */
    public String decrypt(String encryptedDataBase64, String keyBase64) {
        return AesService.decrypt(encryptedDataBase64, keyBase64);
    }

    /**
     * Generates a new RSA key pair for encryption and decryption.
     * @return the generated RSA public/private key pair as Base64-encoded strings.
     */
    public RsaService.RsaKeyPair generateRsaKeyPair() {
        return RsaService.generateRsaKeyPair();
    }

    /**
     * Encrypts the provided data using the specified RSA public key.
     * @param dataBase64 the data to encrypt, as a Base64-encoded string.
     * @param publicKeyBase64 the RSA public key, as a Base64-encoded string.
     * @return the encrypted data as a Base64-encoded string.
     */
    public String encryptRsa(String dataBase64, String publicKeyBase64) {
        return RsaService.encrypt(dataBase64, publicKeyBase64);
    }

    /**
     * Decrypts the provided encrypted data using the specified RSA private key.
     * @param encryptedDataBase64 the encrypted data, as a Base64-encoded string.
     * @param privateKeyBase64 the RSA private key, as a Base64-encoded string.
     * @return the decrypted data as a Base64-encoded string.
     */
    public String decryptRsa(String encryptedDataBase64, String privateKeyBase64) {
        return RsaService.decrypt(encryptedDataBase64, privateKeyBase64);
    }
}
