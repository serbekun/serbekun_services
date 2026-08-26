/**
 * A batch response: one result per requested item, in the order they were asked for.
 *
 * @param items the results
 */
package com.serbekun.ss.domain.dto.http.id;

import java.util.List;

public record V0IdBatchResponse(List<V0IdResponse> items) {}
