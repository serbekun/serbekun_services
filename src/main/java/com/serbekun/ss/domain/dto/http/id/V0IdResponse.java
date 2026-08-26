/**
 * The result of any generation call. Fields that do not apply to the kind of
 * value asked for are left out of the response rather than sent as null.
 *
 * @param type what was generated
 * @param count how many values came back
 * @param values the values; always an array, even when one was asked for
 * @param format how the values were written, where that was a choice
 * @param version the uuid version, for uuids only
 * @param alphabet the alphabet a token was drawn from, for tokens only
 * @param length the length asked for, for tokens and bytes only
 * @param bits how much randomness one value carries
 */
package com.serbekun.ss.domain.dto.http.id;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record V0IdResponse(
    String type,
    int count,
    List<String> values,
    String format,
    String version,
    String alphabet,
    Integer length,
    int bits) {}
