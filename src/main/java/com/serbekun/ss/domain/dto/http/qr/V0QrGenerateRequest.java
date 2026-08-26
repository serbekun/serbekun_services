/** Record for a QR code generation request payload. */
package com.serbekun.ss.domain.dto.http.qr;

public record V0QrGenerateRequest(
    String data,
    String format,
    Integer size,
    String errorCorrection,
    String foreground,
    String background,
    Integer margin) {}
