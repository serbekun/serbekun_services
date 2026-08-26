/**
 * The two documents to compare. Both have to be valid JSON to be sent at all,
 * which is why this endpoint takes a wrapper while the others take the document
 * as the whole body.
 *
 * @param from the document as it is
 * @param to the document as it should be
 */
package com.serbekun.ss.domain.dto.http.json;

import com.fasterxml.jackson.databind.JsonNode;

public record V0JsonDiffRequest(JsonNode from, JsonNode to) {}
