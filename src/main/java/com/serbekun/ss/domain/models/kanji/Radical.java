package com.serbekun.ss.domain.models.kanji;

/**
 * 部首 (radical) of a kanji together with its reading.
 *
 * @param kanji   the radical character(s), e.g. "心"
 * @param reading the reading shown in parentheses, e.g. "こころ・りっしんべん・したごころ"
 */
public record Radical(String kanji, String reading) {
}
