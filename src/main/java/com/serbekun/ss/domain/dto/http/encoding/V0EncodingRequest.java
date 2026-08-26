/**
 * Record for the six fixed encode/decode endpoints, where one side of the
 * conversion is named by the path.
 *
 * @param data the payload
 * @param encoding on an <b>encode</b> call, how to read {@code data}; defaults to {@code utf8}
 * @param outputEncoding on a <b>decode</b> call, how to write the result; defaults to {@code utf8}
 * @param form on the url endpoints, true to use {@code x-www-form-urlencoded}
 *             (a space is {@code +}) rather than RFC 3986
 */
package com.serbekun.ss.domain.dto.http.encoding;

public record V0EncodingRequest(String data, String encoding, String outputEncoding, Boolean form) {}
