package com.serbekun.ss.domain.models.kanji;

/**
 * A single numbered meaning (意味).
 *
 * @param number the meaning index (may be {@code null} if not present on the page)
 * @param text   the meaning text
 */
public record Meaning(Integer number, String text) {
}
