/** Record for hybrid (AES-GCM + RSA-OAEP) decryption request payload. */
package com.serbekun.ss.domain.dto.http.cipher.hybrid;

public record V0CipherHybridPostDecrypt(String data, String encryptedKey, String privateKey) {}
