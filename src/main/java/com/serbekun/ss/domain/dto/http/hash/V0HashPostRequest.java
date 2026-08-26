/** Record for a hashing request payload. */
package com.serbekun.ss.domain.dto.http.hash;

public record V0HashPostRequest(String algorithm, String data, String encoding, String key) {}
