/** Record for a request that shortens a url and renders the short link as a QR code. */
package com.serbekun.ss.domain.dto.http.shorturl;

public record V0ShortUrlQrRequest(
    String url,
    String name,
    String description,
    String baseUrl,
    String format,
    Integer size,
    String errorCorrection,
    String foreground,
    String background,
    Integer margin) {}
