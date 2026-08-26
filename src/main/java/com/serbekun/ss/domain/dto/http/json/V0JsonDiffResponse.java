/**
 * The difference between two documents.
 *
 * @param equal whether they hold the same value
 * @param operations how many operations the patch holds
 * @param patch the RFC 6902 patch that turns {@code from} into {@code to}
 */
package com.serbekun.ss.domain.dto.http.json;

import com.fasterxml.jackson.databind.JsonNode;

public record V0JsonDiffResponse(boolean equal, int operations, JsonNode patch) {}
