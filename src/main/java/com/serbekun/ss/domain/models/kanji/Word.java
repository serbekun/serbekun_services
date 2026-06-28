package com.serbekun.ss.domain.models.kanji;

/**
 * A vocabulary entry / compound (熟語) using the kanji.
 *
 * @param word    the compound word, e.g. "富士山"
 * @param reading the reading (brackets 【】 already stripped), e.g. "ふじさん"
 * @param meaning the meaning / explanation
 */
public record Word(String word, String reading, String meaning) {
}
