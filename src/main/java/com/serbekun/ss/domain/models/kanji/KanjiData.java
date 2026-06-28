package com.serbekun.ss.domain.models.kanji;

import java.util.List;

/**
 * Root data object describing a parsed kanji page — the Java equivalent of the
 * dict returned by the original {@code parse_kanji_page} Python function.
 *
 * <p>List fields ({@code meanings}, {@code words}) are never {@code null}.
 * {@code requestedKanji} and {@code fetchedAt} are populated only when the data
 * was obtained via {@code KanjiPageParser.fetch(...)}; they are {@code null} when
 * parsing raw HTML directly.
 *
 * @param kanji          対象の漢字 — the kanji symbol read from the page
 * @param meaningsShort  短い意味のまとめ — short summary of meanings
 * @param basicInfo      基本情報 — radical, strokes, level, grade, categories, JIS
 * @param readings       読み方 — on-yomi / kun-yomi
 * @param meanings       詳細な意味 — numbered detailed meanings
 * @param words          熟語 — example compounds
 * @param requestedKanji echo of the input passed to {@code fetch(...)}
 * @param fetchedAt      ISO-8601 timestamp of when the page was fetched
 */
public record KanjiData(
        String kanji,
        String meaningsShort,
        BasicInfo basicInfo,
        Readings readings,
        List<Meaning> meanings,
        List<Word> words,
        String requestedKanji,
        String fetchedAt
) {
}
