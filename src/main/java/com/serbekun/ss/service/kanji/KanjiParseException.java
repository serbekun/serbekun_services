package com.serbekun.ss.service.kanji;

/**
 * Thrown when fetching the kanji page from the server fails
 * (network error, timeout, non-2xx HTTP status, etc.).
 */
public class KanjiParseException extends RuntimeException {

    public KanjiParseException(String message) {
        super(message);
    }

    public KanjiParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
