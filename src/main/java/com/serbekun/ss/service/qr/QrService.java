package com.serbekun.ss.service.qr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QR code generation and reading. Stateless — nothing is stored, an image is
 * built per request and handed straight back.
 * <p>
 * Both formats come from the same module matrix. PNG is rendered at the
 * requested pixel size; SVG is rendered from the unscaled module grid and
 * scaled by its {@code viewBox}, so the file stays small and stays sharp at
 * any size.
 */
public class QrService {

    /** Smallest image edge accepted, in pixels — below this a phone camera struggles. */
    public static final int MIN_SIZE = 64;

    /** Largest image edge accepted, in pixels. */
    public static final int MAX_SIZE = 4096;

    /** Edge used when the caller does not ask for one. */
    public static final int DEFAULT_SIZE = 512;

    /** Widest quiet zone accepted, in modules. */
    public static final int MAX_MARGIN = 32;

    /** Quiet zone the spec asks for: four modules of blank space on every side. */
    public static final int DEFAULT_MARGIN = 4;

    /**
     * Longest payload a QR code can hold at all — version 40, error correction
     * L, byte mode. Anything longer is rejected before ZXing has to say so.
     */
    public static final int MAX_DATA_BYTES = 2953;

    /** Rendering options for one generated code. Colors are packed ARGB. */
    public record QrOptions(
            QrFormat format,
            int size,
            QrErrorCorrection errorCorrection,
            int foreground,
            int background,
            int margin) {

        /**
         * Reads caller-supplied options, filling in defaults and validating.
         *
         * @param format {@code png} or {@code svg}; blank means png
         * @param size the image edge in pixels; null means {@value #DEFAULT_SIZE}
         * @param errorCorrection {@code L}, {@code M}, {@code Q} or {@code H}; blank means M
         * @param foreground the module color as {@code #rgb}, {@code #rrggbb} or
         *                   {@code #rrggbbaa}; blank means black
         * @param background the backdrop color in the same notation; blank means white
         * @param margin the quiet zone in modules; null means {@value #DEFAULT_MARGIN}
         * @return the validated options
         * @throws IllegalArgumentException when any value is out of range or unparsable
         */
        public static QrOptions of(String format, Integer size, String errorCorrection,
                String foreground, String background, Integer margin) {
            int edge = size == null ? DEFAULT_SIZE : size;
            if (edge < MIN_SIZE || edge > MAX_SIZE) {
                throw new IllegalArgumentException(
                    "size must be between " + MIN_SIZE + " and " + MAX_SIZE + " pixels");
            }

            int quietZone = margin == null ? DEFAULT_MARGIN : margin;
            if (quietZone < 0 || quietZone > MAX_MARGIN) {
                throw new IllegalArgumentException("margin must be between 0 and " + MAX_MARGIN + " modules");
            }

            return new QrOptions(
                QrFormat.fromWire(format),
                edge,
                QrErrorCorrection.fromWire(errorCorrection),
                parseColor(foreground, 0xFF000000, "foreground"),
                parseColor(background, 0xFFFFFFFF, "background"),
                quietZone);
        }
    }

    /**
     * A rendered code.
     *
     * @param format the format it was rendered in
     * @param content the image bytes — PNG data, or UTF-8 SVG markup
     * @param size the image edge in pixels
     */
    public record QrImage(QrFormat format, byte[] content, int size) {

        /** The MIME type this image should be served with. */
        public String contentType() {
            return format.contentType();
        }

        /** The image as a {@code data:} URL, for callers that embed it rather than download it. */
        public String dataUrl() {
            return "data:" + contentType() + ";base64," + Base64.getEncoder().encodeToString(content);
        }
    }

    /**
     * The outcome of reading an image.
     *
     * @param found whether a code was located at all
     * @param text the decoded text, or null when nothing was found
     * @param format the barcode format that was decoded, or null when nothing was found
     */
    public record QrRead(boolean found, String text, String format) {

        /** The answer when an image simply holds no code — not an error. */
        static QrRead notFound() {
            return new QrRead(false, null, null);
        }
    }

    /**
     * Renders {@code data} as a QR code.
     *
     * @param data the payload to encode; may be any text, must not be blank
     * @param options how to render it
     * @return the rendered image
     * @throws IllegalArgumentException when the payload is missing, or too long to encode
     */
    public QrImage generate(String data, QrOptions options) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("data is required");
        }

        int bytes = data.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > MAX_DATA_BYTES) {
            throw new IllegalArgumentException(
                "data is too long for a QR code: " + bytes + " bytes, the maximum is " + MAX_DATA_BYTES);
        }

        return switch (options.format()) {
            case PNG -> new QrImage(QrFormat.PNG, png(data, options), options.size());
            case SVG -> new QrImage(QrFormat.SVG,
                svg(encode(data, options, 1), options).getBytes(StandardCharsets.UTF_8), options.size());
        };
    }

    /**
     * Reads a QR code out of an image.
     *
     * @param image the raw bytes of a PNG, JPEG, GIF or BMP
     * @return what was decoded, or {@link QrRead#notFound()} when the image holds no code
     * @throws IllegalArgumentException when the bytes are not an image any decoder here understands
     */
    public QrRead read(byte[] image) {
        if (image == null || image.length == 0) {
            throw new IllegalArgumentException("an image is required");
        }

        BufferedImage buffered;
        try {
            buffered = ImageIO.read(new ByteArrayInputStream(image));
        } catch (IOException e) {
            throw new IllegalArgumentException("the image could not be read");
        }
        if (buffered == null) {
            throw new IllegalArgumentException(
                "unsupported image format — send a PNG, JPEG, GIF or BMP");
        }

        LuminanceSource source = new BufferedImageLuminanceSource(buffered);

        // Two passes: the plain one, then the inverted one, because a code
        // printed light-on-dark is a perfectly ordinary thing to be handed.
        Result result = decode(source);
        if (result == null) {
            result = decode(source.invert());
        }
        return result == null
            ? QrRead.notFound()
            : new QrRead(true, result.getText(), result.getBarcodeFormat().toString());
    }

    // region generation

    /** Renders the code as PNG at the requested pixel size. */
    private static byte[] png(String data, QrOptions options) {
        BitMatrix matrix = encode(data, options, options.size());
        MatrixToImageConfig colors = new MatrixToImageConfig(options.foreground(), options.background());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out, colors);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render the QR code as PNG", e);
        }
        return out.toByteArray();
    }

    /**
     * Renders the module matrix as SVG.
     * <p>
     * Dark modules are emitted as one path of horizontal runs rather than a
     * rect per module — a dense code has thousands of them, and a run of eight
     * costs the same eight characters as a single one would.
     */
    private static String svg(BitMatrix matrix, QrOptions options) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        StringBuilder path = new StringBuilder();
        for (int y = 0; y < height; y++) {
            int runStart = -1;
            for (int x = 0; x <= width; x++) {
                boolean dark = x < width && matrix.get(x, y);
                if (dark && runStart < 0) {
                    runStart = x;
                } else if (!dark && runStart >= 0) {
                    path.append('M').append(runStart).append(' ').append(y)
                        .append('h').append(x - runStart).append("v1h-").append(x - runStart).append('z');
                    runStart = -1;
                }
            }
        }

        StringBuilder svg = new StringBuilder(path.length() + 512);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(options.size())
           .append("\" height=\"").append(options.size())
           .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
           .append("\" shape-rendering=\"crispEdges\" role=\"img\">");

        if (alpha(options.background()) > 0) {
            svg.append("<rect width=\"").append(width).append("\" height=\"").append(height).append('"');
            appendFill(svg, options.background());
            svg.append("/>");
        }

        svg.append("<path");
        appendFill(svg, options.foreground());
        svg.append(" d=\"").append(path).append("\"/>");

        return svg.append("</svg>").toString();
    }

    /** Writes an ARGB color as a {@code fill}, with {@code fill-opacity} only when it is needed. */
    private static void appendFill(StringBuilder svg, int argb) {
        svg.append(" fill=\"#").append(String.format(Locale.ROOT, "%06x", argb & 0xFFFFFF)).append('"');

        int alpha = alpha(argb);
        if (alpha < 255) {
            svg.append(" fill-opacity=\"")
               .append(String.format(Locale.ROOT, "%.3f", alpha / 255.0))
               .append('"');
        }
    }

    /**
     * Encodes the payload into a module matrix.
     *
     * @param edge the pixel edge to render at; 1 asks ZXing for the unscaled
     *             module grid, which is what the SVG path is built from
     */
    private static BitMatrix encode(String data, QrOptions options, int edge) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, options.errorCorrection().level());
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, options.margin());

        try {
            return new QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, edge, edge, hints);
        } catch (WriterException e) {
            // The only thing that fails here is a payload the chosen error
            // correction level cannot fit — which is the caller's to fix.
            throw new IllegalArgumentException(
                "data does not fit in a QR code at error correction level "
                    + options.errorCorrection().wireName());
        }
    }

    // endregion

    // region reading

    /** One decode attempt; null rather than an exception when nothing is there. */
    private static Result decode(LuminanceSource source) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.List.of(BarcodeFormat.QR_CODE));

        try {
            return new MultiFormatReader().decode(
                new BinaryBitmap(new HybridBinarizer(source)), hints);
        } catch (NotFoundException e) {
            return null;
        }
    }

    // endregion

    // region colors

    /**
     * Reads a CSS-style hex color into packed ARGB.
     *
     * @param value {@code #rgb}, {@code #rgba}, {@code #rrggbb} or {@code #rrggbbaa};
     *              blank falls back to {@code fallback}
     * @param fallback the packed ARGB to use when nothing was given
     * @param field the field name, so an error says which color was wrong
     * @return the packed ARGB value
     * @throws IllegalArgumentException when the value is not a hex color
     */
    static int parseColor(String value, int fallback, String field) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String hex = value.strip().toLowerCase(Locale.ROOT);
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("[0-9a-f]+")) {
            throw new IllegalArgumentException(field + " must be a hex color like #000000");
        }

        // The three and four digit forms are shorthand: each digit is doubled.
        if (hex.length() == 3 || hex.length() == 4) {
            StringBuilder expanded = new StringBuilder(hex.length() * 2);
            for (int i = 0; i < hex.length(); i++) {
                expanded.append(hex.charAt(i)).append(hex.charAt(i));
            }
            hex = expanded.toString();
        }

        return switch (hex.length()) {
            case 6 -> 0xFF000000 | (int) Long.parseLong(hex, 16);
            case 8 -> (int) Long.parseLong(hex.substring(6) + hex.substring(0, 6), 16);
            default -> throw new IllegalArgumentException(
                field + " must be a hex color like #000000, #fff or #00000000");
        };
    }

    /** The alpha channel of a packed ARGB value. */
    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    // endregion
}
