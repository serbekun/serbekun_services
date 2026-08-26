/** Record for RSA encryption request payload. */
package com.serbekun.ss.domain.dto.http.cipher.rsa;

public record V0CipherRsaPostEncrypt(String data, String publicKey) {}
