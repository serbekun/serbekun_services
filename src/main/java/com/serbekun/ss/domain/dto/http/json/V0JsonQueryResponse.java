/**
 * What a query found.
 *
 * @param expression the expression as written by the caller
 * @param syntax which language it was read as: {@code pointer} or {@code jsonpath}
 * @param count how many matches there were; zero is a normal answer
 * @param matches the values found, in document order
 * @param paths where each match was found, in the same syntax as the query
 */
package com.serbekun.ss.domain.dto.http.json;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record V0JsonQueryResponse(
    String expression,
    String syntax,
    int count,
    JsonNode matches,
    List<String> paths) {}
