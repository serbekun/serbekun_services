package com.serbekun.ss.service.http.handles.v0;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serbekun.ss.service.cipher.AesService;

// TODO add slf4j logger

/**
 * implement request to /v0/api/cipher/aes/*
 */
public class V0ApiCipherAes {

    private final ObjectMapper mapper = new ObjectMapper();

    public V0ApiCipherAes() {}

    /**
     * 
     * implement get request return aes key.
     * 
     * @return generated key json response
     */
    public String get() {

        String generatedKey = AesService.generateAesKey();
        GetResponseJson getResponseJson = new GetResponseJson(generatedKey);
        
        try {
            String json = mapper.writeValueAsString(getResponseJson);
            return json;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 
     * implement /aes/encrypt request
     * encrypt data
     * 
     * @param data data base64 string that will be encrypted.
     * @param key key that will be used to encrypt data.
     * @return encrypted data response json
     */
    public String postEncrypt(String data, String key) {

        String encryptedBase64 = AesService.encrypt(data, key);
        PostEncryptResponseJson postEncryptResponseJson = new PostEncryptResponseJson(encryptedBase64);
        try {
            String json = mapper.writeValueAsString(postEncryptResponseJson);
            return json;

        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 
     * implement /aes/decrypt request
     * decrypt data
     * 
     * @param data data base64 string that will be decrypted
     * @param key key that will be used to decrypt data
     * @return decrypted data response json
     */
    public String postDecrypt(String data, String key) {
        String decryptedBase64 = AesService.decrypt(data, key);
        PostDecryptResponseJson postDecryptResponseJson = new PostDecryptResponseJson(decryptedBase64);
        try {
            String json = mapper.writeValueAsString(postDecryptResponseJson);
            return json;
        } catch (JsonProcessingException e) {
            return null;
        } 
    }

    // JSON String response dto

    /**
     * record for make json use jackson for get response
     */
    private record GetResponseJson(String key) {}

    /**
     * record for make json use jackson for post encrypt response
     */
    private record PostEncryptResponseJson(String data) {}

    /**
     * record for make json use jackson for post decrypt response
     */
    private record PostDecryptResponseJson(String data) {}

}