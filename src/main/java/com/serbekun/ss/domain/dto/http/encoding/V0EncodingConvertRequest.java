/**
 * Record for a conversion between any two formats.
 *
 * @param data the payload as written in {@code from}
 * @param from the format to read it as
 * @param to the format to write it in
 */
package com.serbekun.ss.domain.dto.http.encoding;

public record V0EncodingConvertRequest(String data, String from, String to) {}
