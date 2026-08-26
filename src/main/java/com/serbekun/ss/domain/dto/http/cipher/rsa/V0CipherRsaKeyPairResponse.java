/** Response DTO carrying a freshly generated RSA key pair. */
package com.serbekun.ss.domain.dto.http.cipher.rsa;

public record V0CipherRsaKeyPairResponse(String publicKey, String privateKey) {}
