/**
 * Response DTO for a short url created together with its QR code.
 *
 * @param id the short id
 * @param token the delete token — shown only here
 * @param shortUrl the full link the QR code points at
 * @param format the format the code was rendered in
 * @param contentType the MIME type of the embedded image
 * @param size the image edge in pixels
 * @param qr the code as a {@code data:} URL, ready to drop into an {@code <img src>}
 */
package com.serbekun.ss.domain.dto.http.shorturl;

public record V0ShortUrlQrResponse(
    String id,
    String token,
    String shortUrl,
    String format,
    String contentType,
    int size,
    String qr) {}
