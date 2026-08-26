/** Record for a QR code read request carrying the image as base64 or a {@code data:} URL. */
package com.serbekun.ss.domain.dto.http.qr;

public record V0QrReadRequest(String image) {}
