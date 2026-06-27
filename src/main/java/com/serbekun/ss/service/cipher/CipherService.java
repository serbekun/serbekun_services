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
}