/**
 * The result of an encode, decode or convert call. The same shape answers all
 * seven endpoints — every one of them is a conversion between two formats.
 *
 * @param data the payload written in the target format
 * @param bytes how many bytes the payload is, whatever it is written in
 * @param from the format the input was read as
 * @param to the format the output was written in
 */
package com.serbekun.ss.domain.dto.http.encoding;

public record V0EncodingResponse(String data, long bytes, String from, String to) {}
