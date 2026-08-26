/**
 * JSON view of a generated QR code, returned instead of the raw image when the
 * caller asks for {@code application/json}.
 *
 * @param format the format the code was rendered in
 * @param contentType the MIME type of the embedded image
 * @param size the image edge in pixels
 * @param image the image as a {@code data:} URL, ready to drop into an {@code <img src>}
 */
package com.serbekun.ss.domain.dto.http.qr;

public record V0QrGenerateResponse(String format, String contentType, int size, String image) {}
