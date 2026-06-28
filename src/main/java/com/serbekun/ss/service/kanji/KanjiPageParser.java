package com.serbekun.ss.service.kanji;

import com.serbekun.ss.domain.models.kanji.BasicInfo;
import com.serbekun.ss.domain.models.kanji.KanjiData;
import com.serbekun.ss.domain.models.kanji.Meaning;
import com.serbekun.ss.domain.models.kanji.Radical;
import com.serbekun.ss.domain.models.kanji.Readings;
import com.serbekun.ss.domain.models.kanji.Word;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Fetches kanji pages from a server (e.g. {@code https://kanji.me/}) and parses
 * their HTML into a structured {@link KanjiData} object.
 *
 * <p>Java port of the original Python {@code Parser.parse_kanji_page} +
 * {@code __main__.get_kanji} logic. The {@code baseUrl} is supplied once in the
 * constructor; afterwards call {@link #fetch(String)} for a kanji, or
 * {@link #parse(String)} to parse HTML you already have.
 */
public class KanjiPageParser {

    private static final Pattern BRACKETS = Pattern.compile("[【】]");
    private static final String USER_AGENT = "Kanji-API/0.1 (Java; compatible)";

    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient httpClient;

    /** Uses a default 12-second timeout */
    public KanjiPageParser(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(12));
    }

    public KanjiPageParser(String baseUrl, Duration timeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be empty");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }


    // region public API

    /**
     * Fetches {@code baseUrl + kanji} from the server and parses the response
     * into a {@link KanjiData}. The result has {@code requestedKanji} and
     * {@code fetchedAt} populated.
     *
     * @throws IllegalArgumentException if {@code kanji} is empty or longer than 5 chars
     * @throws KanjiParseException      on any network / HTTP failure
     */
    public KanjiData fetch(String kanji) {
        if (kanji == null || kanji.isBlank() || kanji.strip().length() > 5) {
            throw new IllegalArgumentException("Expected 1-5 kanji characters");
        }
        String html = fetchHtml(kanji.strip());
        return parse(html, kanji.strip(), Instant.now().toString());
    }

    /**
     * Parses raw HTML (no network access). {@code requestedKanji} and
     * {@code fetchedAt} in the result will be {@code null}.
     */
    public KanjiData parse(String html) {
        return parse(html, null, null);
    }

    // endregion

    // region fetching

    /**
     * Fetches the HTML for a kanji page from the server. Throws
     * @param kanji the kanji to fetch
     * @return the HTML response body
     * @throws KanjiParseException on any network / HTTP failure
     */
    private String fetchHtml(String kanji) {
        String encoded = URLEncoder.encode(kanji, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + encoded))
                .timeout(timeout)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html")
                .GET()
                .build();
        try {
            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = resp.statusCode();
            if (status >= 400) {
                throw new KanjiParseException("Server returned HTTP " + status + " for kanji: " + kanji);
            }
            return resp.body();
        } catch (IOException e) {
            throw new KanjiParseException("Unable to fetch kanji page: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KanjiParseException("Request interrupted while fetching: " + kanji, e);
        }
    }

    // endregion

    // region parse

    /**
     * Parses the HTML of a kanji page into a {@link KanjiData} object. The
     * @param html HTML response body
     * @param requestedKanji the kanji that was requested (may be null)
     * @param fetchedAt the timestamp of when the page was fetched (may be null)
     * @return a structured {@link KanjiData} object
     */
    private KanjiData parse(String html, String requestedKanji, String fetchedAt) {
        Document doc = Jsoup.parse(html);
        return new KanjiData(
                getKanji(doc),
                getMeaningsShort(doc),
                getBasicInfo(doc),
                getReadings(doc),
                getMeanings(doc),
                getWords(doc),
                requestedKanji,
                fetchedAt
        );
    }

    /** Extracts the kanji symbol from {@code <main data-kanji="...">}, falling back to the breadcrumb.
     *  @param doc the parsed HTML document
     *  @return the kanji symbol, or null if not found
     */
    private static String getKanji(Document doc) {
        Element main = doc.selectFirst("main[data-kanji]");
        if (main != null) {
            return main.attr("data-kanji");
        }
        Element span = doc.selectFirst("nav span.japanese-text");
        return span != null ? span.text() : null;
    }

    /** Short meaning text beneath the primary content. 
     * @param doc the parsed HTML document
     * @return the short meaning text, or null if not found
     */
    private static String getMeaningsShort(Document doc) {
        Element section = doc.selectFirst("section#basic-info");
        if (section == null) {
            return null;
        }
        Element div = section.selectFirst("div.text-lg.font-bold");
        return div != null ? div.text() : null;
    }

    /** Parses the Basic Information section. 
     * @param doc the parsed HTML document
     * @return a BasicInfo object with the extracted data
     */
    private static BasicInfo getBasicInfo(Document doc) {
        Element section = doc.selectFirst("section#basic-info");
        if (section == null) {
            return new BasicInfo(null, null, null, null, List.of(), null);
        }

        Radical radical = null;
        Integer strokeCount = null;
        String kankenLevel = null;
        String grade = null;
        List<String> categories = new ArrayList<>();
        String jisLevel = null;

        for (Element item : section.select("div.info-item")) {
            Element labelEl = item.selectFirst("span.info-label");
            if (labelEl == null) {
                continue;
            }
            switch (labelEl.text()) {
                case "部首" -> {
                    Element kanjiSpan = item.selectFirst("span.japanese-text");
                    Element readingSpan = item.selectFirst("div.radical-content span.text-xs");
                    radical = new Radical(
                            kanjiSpan != null ? kanjiSpan.text() : null,
                            readingSpan != null ? readingSpan.text() : null
                    );
                }
                case "画数" -> {
                    Element a = item.selectFirst("a");
                    if (a != null) {
                        strokeCount = parseIntSafe(a.text());
                    }
                }
                case "漢検" -> {
                    Element a = item.selectFirst("a");
                    kankenLevel = a != null ? a.text() : null;
                }
                case "学年" -> {
                    Element a = item.selectFirst("a");
                    grade = a != null ? a.text() : null;
                }
                case "種別" -> {
                    for (Element a : item.select("a")) {
                        categories.add(a.text());
                    }
                }
                case "JIS水準" -> {
                    Element span = item.selectFirst("span:not(.info-label)");
                    jisLevel = span != null ? span.text() : null;
                }
                default -> {
                    // unknown label — ignore
                }
            }
        }
        return new BasicInfo(radical, strokeCount, kankenLevel, grade, categories, jisLevel);
    }

    /** Processes the Readings section and categorizes on-yomi / kun-yomi. 
     * @param doc the parsed HTML document
     * @return a Readings object containing lists of on-yomi and kun-yomi readings
     */
    private static Readings getReadings(Document doc) {
        List<String> onYomi = new ArrayList<>();
        List<String> kunYomi = new ArrayList<>();

        Element section = doc.selectFirst("section#readings");
        if (section == null) {
            return new Readings(onYomi, kunYomi);
        }

        for (Element div : section.select("div.rounded-lg")) {
            Element labelEl = div.selectFirst("div.text-xs.font-medium");
            if (labelEl == null) {
                continue;
            }
            String label = labelEl.text();
            List<String> readings = new ArrayList<>();
            for (Element btn : div.select("button.reading-sound-btn")) {
                Element rt = btn.selectFirst("span.reading-text");
                if (rt != null) {
                    readings.add(rt.text());
                }
            }
            if (label.contains("音読み")) {
                onYomi = readings;
            } else if (label.contains("訓読み")) {
                kunYomi = readings;
            }
        }
        return new Readings(onYomi, kunYomi);
    }

    /** Extracts numbered meanings from the Meanings section. 
     * @param doc the parsed HTML document
     * @return a list of Meaning objects, each with an optional number and text
     */
    private static List<Meaning> getMeanings(Document doc) {
        List<Meaning> meanings = new ArrayList<>();
        Element section = doc.selectFirst("section#meanings");
        if (section == null) {
            return meanings;
        }
        for (Element item : section.select("div.meaning-item")) {
            Element numEl = item.selectFirst("span.meaning-number-inner");
            Element textEl = item.selectFirst("p");
            if (textEl != null) {
                Integer number = numEl != null ? parseIntSafe(numEl.text()) : null;
                meanings.add(new Meaning(number, textEl.text()));
            }
        }
        return meanings;
    }

    /** Extracts word compounds with kanji, reading, and meaning. 
     * @param doc the parsed HTML document
     * @return a list of Word objects
     */
    private static List<Word> getWords(Document doc) {
        List<Word> words = new ArrayList<>();
        Element section = doc.selectFirst("section#words");
        if (section == null) {
            return words;
        }
        for (Element card : section.select("div.word-card")) {
            StringBuilder word = new StringBuilder();
            for (Element a : card.select("a.kanji-char")) {
                word.append(a.text());
            }

            String reading = "";
            Element readingDiv = card.selectFirst("div.word-reading");
            if (readingDiv != null) {
                reading = BRACKETS.matcher(readingDiv.text()).replaceAll("").strip();
            }

            Element meaningDiv = card.selectFirst("div.word-meaning");
            String meaning = meaningDiv != null ? meaningDiv.text() : "";

            if (!word.isEmpty()) {
                words.add(new Word(word.toString(), reading, meaning));
            }
        }
        return words;
    }

    // endregion

    // region helpers

    /** Parses the leading integer out of a string (e.g. "3画" -> 3); returns null if none. 
     * @param s the string to parse
     * @return the integer value, or null if not found
     */
    private static Integer parseIntSafe(String s) {
        if (s == null) {
            return null;
        }
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // endregion
}
