package com.serbekun.ss.service.cipher;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;   // bytes, recommended IV size for GCM
    private static final int GCM_TAG_LENGTH = 128; // bits, authentication tag length

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate AES key and return base64 string of key
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
     * Uses AES/GCM with a fresh random IV per call, so the same plaintext
     * produces different cipher text every time. The IV is prepended to the
     * cipher text before base64-encoding the result.
     *
     * @param dataBase64 the plain data in base64
     * @param keyBase64 the AES key in base64
     * @return encrypted data (IV || cipher text+tag) in base64
     */
    public static String encrypt(String dataBase64, String keyBase64) {
        try {
            byte[] dataBytes = Base64.getDecoder().decode(dataBase64);
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encryptedBytes = cipher.doFinal(dataBytes);

            byte[] result = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, result, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts data from base64 string using AES key in base64.
     * Expects the input produced by {@link #encrypt(String, String)}:
     * a base64 string of (IV || cipher text+tag). Splits off the leading IV,
     * verifies the GCM authentication tag and returns the plaintext.
     *
     * @param encryptedDataBase64 the encrypted data in base64
     * @param keyBase64 the AES key in base64
     * @return decrypted data in base64
     */
    public static String decrypt(String encryptedDataBase64, String keyBase64) {
        try {
            byte[] cipherMessage = Base64.getDecoder().decode(encryptedDataBase64);
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);

            if (cipherMessage.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted data is too short");
            }

            byte[] iv = Arrays.copyOfRange(cipherMessage, 0, GCM_IV_LENGTH);
            byte[] encryptedBytes = Arrays.copyOfRange(cipherMessage, GCM_IV_LENGTH, cipherMessage.length);

            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return Base64.getEncoder().encodeToString(decryptedBytes);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
