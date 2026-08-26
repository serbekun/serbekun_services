/**
 * A batch request. A body that is a single item on its own is accepted too, so
 * a caller with one awkward request does not have to wrap it.
 *
 * @param items what to generate, in order
 */
package com.serbekun.ss.domain.dto.http.id;

import java.util.List;

public record V0IdBatchRequest(List<V0IdBatchItem> items) {}
