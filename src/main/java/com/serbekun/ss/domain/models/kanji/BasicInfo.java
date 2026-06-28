package com.serbekun.ss.domain.models.kanji;

import java.util.List;

/**
 * Basic information block (基本情報) of a kanji page.
 * Scalar fields are {@code null} when the corresponding label is absent on the page;
 * {@code categories} is never {@code null} (empty list instead).
 *
 * @param radical      部首 — radical character and reading
 * @param strokeCount  画数 — number of strokes
 * @param kankenLevel  漢検 — Kanji Kentei (Kanken) exam level, e.g. "10級"
 * @param grade        学年 — Japanese school grade, e.g. "1年生"
 * @param categories   種別 — classifications, e.g. ["教育漢字", "常用漢字"]
 * @param jisLevel     JIS水準 — JIS standard level, e.g. "第1水準"
 */
public record BasicInfo(
        Radical radical,
        Integer strokeCount,
        String kankenLevel,
        String grade,
        List<String> categories,
        String jisLevel
) {
}
