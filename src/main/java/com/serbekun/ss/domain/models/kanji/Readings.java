package com.serbekun.ss.domain.models.kanji;

import java.util.List;

/**
 * Readings section (読み方). Both lists are never {@code null} (empty list when absent).
 *
 * @param onYomi  音読み — Chinese-derived (on) readings
 * @param kunYomi 訓読み — native Japanese (kun) readings
 */
public record Readings(List<String> onYomi, List<String> kunYomi) {
}
