/** Record for an integrity check request payload. */
package com.serbekun.ss.domain.dto.http.hash;

public record V0HashVerifyRequest(String algorithm, String data, String encoding, String key, String hash) {}
