package com.serbekun.ss.service.cipher;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AesService {
    
    /**
     *
     * generate AES key and return base64 string of key
     *
     * @return base64 AES key string
     */
    public static String generateAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Encrypts data from base64 string using AES key in base64.
     * Decodes base64 data to bytes, encrypts, returns encrypted bytes as base64.
     *
     * @param dataBase64 the plain data in base64
     * @param keyBase64 the AES key in base64
     * @return encrypted data in base64
     */
    public static String encrypt(String dataBase64, String keyBase64) {
        try {
            byte[] dataBytes = Base64.getDecoder().decode(dataBase64);
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedBytes = cipher.doFinal(dataBytes);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts data from base64 string using AES key in base64.
     * Decodes base64 data to bytes, decrypts, returns decrypted bytes as base64.
     *
     * @param encryptedDataBase64 the encrypted data in base64
     * @param keyBase64 the AES key in base64
     * @return decrypted data in base64
     */
    public static String decrypt(String encryptedDataBase64, String keyBase64) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedDataBase64);
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return Base64.getEncoder().encodeToString(decryptedBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
