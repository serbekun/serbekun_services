/** Record for RSA decryption request payload. */
package com.serbekun.ss.domain.dto.http.cipher.rsa;

public record V0CipherRsaPostDecrypt(String data, String privateKey) {}
