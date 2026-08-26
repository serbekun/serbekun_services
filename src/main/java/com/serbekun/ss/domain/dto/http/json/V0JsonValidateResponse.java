/**
 * The outcome of checking a document. A document that does not parse comes back
 * as a 200 with {@code valid: false} — where the problem is, is the answer.
 *
 * @param valid whether it parsed
 * @param error what was wrong; absent when nothing was
 * @param line the 1-based line the problem is on; absent when there is none
 * @param column the 1-based column
 * @param bytes the size of the document as UTF-8
 */
package com.serbekun.ss.domain.dto.http.json;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record V0JsonValidateResponse(
    boolean valid,
    String error,
    Integer line,
    Integer column,
    long bytes) {}
