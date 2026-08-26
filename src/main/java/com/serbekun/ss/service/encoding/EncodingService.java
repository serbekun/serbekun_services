package com.serbekun.ss.service.encoding;

/**
 * Conversion between the representations a payload can be written in — base64,
 * base64url, base32, hex, url percent-encoding and plain UTF-8 text.
 * <p>
 * Everything is one operation: decode the input out of the format it came in,
 * then encode those bytes into the format asked for. The six fixed endpoints
 * are that same conversion with one side pinned, which is why there is only one
 * method here. This is not encryption and not hashing — every step is
 * reversible by anyone, and the point is only to change how bytes are written.
 */
public class EncodingService {

    /**
     * The outcome of a conversion.
     *
     * @param data the payload written in the target format
     * @param bytes how many bytes the payload actually is, whatever it is written in
     * @param from the format the input was read as
     * @param to the format the output was written in
     */
    public record ConvertResult(String data, long bytes, EncodingFormat from, EncodingFormat to) {
    }

    /**
     * Rewrites a payload from one format into another.
     *
     * @param data the payload as written in {@code from}; an empty string is
     *             valid and converts to an empty result
     * @param from the format to read the input as
     * @param to the format to write the output in
     * @return the converted payload and the size it stands for
     * @throws IllegalArgumentException when the input is not valid in {@code from},
     *         or the bytes cannot be written as {@code to} — which only happens
     *         when arbitrary bytes are asked for as UTF-8 text
     */
    public ConvertResult convert(String data, EncodingFormat from, EncodingFormat to) {
        if (data == null) {
            throw new IllegalArgumentException("data is required");
        }

        byte[] decoded = from.decode(data);
        return new ConvertResult(to.encode(decoded), decoded.length, from, to);
    }
}
