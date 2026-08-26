/** Record for RSA signing request payload. */
package com.serbekun.ss.domain.dto.http.cipher.rsa;

public record V0CipherRsaPostSign(String data, String privateKey) {}
