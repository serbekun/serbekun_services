/**
 * The outcome of reading an image.
 *
 * @param found whether a code was located; an image without one is a 200 with
 *              {@code found: false}, not an error
 * @param text the decoded text, null when nothing was found
 * @param format the barcode format that was decoded, null when nothing was found
 */
package com.serbekun.ss.domain.dto.http.qr;

public record V0QrReadResponse(boolean found, String text, String format) {}
