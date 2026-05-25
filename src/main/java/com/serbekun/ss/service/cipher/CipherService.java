package com.serbekun.ss.service.cipher;

public class CipherService {

    public String generateAesKey() {
        return AesService.generateAesKey();
    }

    public String encrypt(String dataBase64, String keyBase64) {
        return AesService.encrypt(dataBase64, keyBase64);
    }

    public String decrypt(String encryptedDataBase64, String keyBase64) {
        return AesService.decrypt(encryptedDataBase64, keyBase64);
    }
}