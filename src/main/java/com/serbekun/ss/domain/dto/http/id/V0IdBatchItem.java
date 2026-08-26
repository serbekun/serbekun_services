/**
 * One item of a batch — the same set of choices the single-purpose endpoints
 * take as query parameters.
 *
 * @param type {@code uuid}, {@code ulid}, {@code token} or {@code bytes}
 * @param count how many to generate; defaults to 1
 * @param version for uuid: {@code v4} or {@code v7}
 * @param format for uuid, ulid and bytes: how to write the values
 * @param length for token: characters; for bytes: bytes
 * @param alphabet for token: a named alphabet
 * @param chars for token: a set of characters to use instead of a named alphabet
 */
package com.serbekun.ss.domain.dto.http.id;

public record V0IdBatchItem(
    String type,
    Integer count,
    String version,
    String format,
    Integer length,
    String alphabet,
    String chars) {}
