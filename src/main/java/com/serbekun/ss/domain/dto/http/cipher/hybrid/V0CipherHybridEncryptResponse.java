/** Response DTO carrying a hybrid cipher text and its RSA-wrapped AES key. */
package com.serbekun.ss.domain.dto.http.cipher.hybrid;

public record V0CipherHybridEncryptResponse(String data, String encryptedKey) {}
