/** Record for RSA signature verification request payload. */
package com.serbekun.ss.domain.dto.http.cipher.rsa;

public record V0CipherRsaPostVerify(String data, String signature, String publicKey) {}
