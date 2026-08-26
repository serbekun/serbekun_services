/** Record for hybrid (AES-GCM + RSA-OAEP) encryption request payload. */
package com.serbekun.ss.domain.dto.http.cipher.hybrid;

public record V0CipherHybridPostEncrypt(String data, String publicKey) {}
