package com.serbekun.ss.service.qr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link QrService}.
 * <p>
 * Wherever it can, a test goes the whole way round — encode, then decode — so
 * a "looks like a QR code" regression cannot pass. The SVG path builder is
 * hand-rolled, so its output is rasterized here and read back too: that is the
 * only way a wrong run length or a shifted row would show up.
 */
class QrServiceTest {

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    private final QrService service = new QrService();

    private static QrService.QrOptions options(String format) {
        return QrService.QrOptions.of(format, null, null, null, null, null);
    }

    // region generate

    @Test
    void pngRoundTripsThroughRead() {
        QrService.QrImage image = service.generate("https://ss.serbekun.com", options("png"));

        assertThat(image.format()).isEqualTo(QrFormat.PNG);
        assertThat(image.contentType()).isEqualTo("image/png");
        assertThat(image.size()).isEqualTo(QrService.DEFAULT_SIZE);
        assertThat(image.content()).startsWith(PNG_MAGIC);

        QrService.QrRead read = service.read(image.content());
        assertThat(read.found()).isTrue();
        assertThat(read.text()).isEqualTo("https://ss.serbekun.com");
        assertThat(read.format()).isEqualTo("QR_CODE");
    }

    @Test
    void pngIsRenderedAtTheRequestedSize() throws Exception {
        QrService.QrImage image = service.generate("size check",
            QrService.QrOptions.of("png", 256, null, null, null, null));

        BufferedImage rendered = ImageIO.read(new ByteArrayInputStream(image.content()));
        assertThat(rendered.getWidth()).isEqualTo(256);
        assertThat(rendered.getHeight()).isEqualTo(256);
    }

    @Test
    void unicodePayloadSurvivesTheRoundTrip() {
        String payload = "Привет, мир — 日本語 ✓";

        QrService.QrRead read = service.read(service.generate(payload, options("png")).content());

        assertThat(read.text()).isEqualTo(payload);
    }

    @Test
    void svgIsMarkupThatScalesByViewBox() {
        QrService.QrImage image = service.generate("https://ss.serbekun.com",
            QrService.QrOptions.of("svg", 640, null, null, null, null));

        String svg = new String(image.content(), StandardCharsets.UTF_8);
        assertThat(image.format()).isEqualTo(QrFormat.SVG);
        assertThat(image.contentType()).isEqualTo("image/svg+xml");
        assertThat(svg).startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\"")
            .contains("width=\"640\" height=\"640\"")
            .contains("shape-rendering=\"crispEdges\"")
            .endsWith("</svg>");

        // The viewBox is the module grid, not the pixel size — that is what
        // keeps the file small however large the code is drawn.
        assertThat(viewBoxWidth(svg)).isLessThan(200);
    }

    @Test
    void svgGeometryDecodesBackToThePayload() {
        String payload = "https://ss.serbekun.com/svg-geometry";
        QrService.QrImage image = service.generate(payload, options("svg"));

        QrService.QrRead read = service.read(rasterize(new String(image.content(), StandardCharsets.UTF_8)));

        assertThat(read.found()).isTrue();
        assertThat(read.text()).isEqualTo(payload);
    }

    @Test
    void marginIsTheQuietZoneInModules() {
        String payload = "quiet zone";
        int withDefault = viewBoxWidth(svgOf(payload, null));
        int withoutMargin = viewBoxWidth(svgOf(payload, 0));

        // Four modules of blank space on each side is the default.
        assertThat(withDefault - withoutMargin).isEqualTo(2 * QrService.DEFAULT_MARGIN);
    }

    @Test
    void higherErrorCorrectionMakesADenserCode() {
        String payload = "the same payload, more redundancy";

        int atL = viewBoxWidth(new String(service.generate(payload,
            QrService.QrOptions.of("svg", null, "L", null, null, null)).content(), StandardCharsets.UTF_8));
        int atH = viewBoxWidth(new String(service.generate(payload,
            QrService.QrOptions.of("svg", null, "H", null, null, null)).content(), StandardCharsets.UTF_8));

        assertThat(atH).isGreaterThan(atL);
    }

    @Test
    void colorsAreApplied() throws Exception {
        QrService.QrImage image = service.generate("colored",
            QrService.QrOptions.of("png", 128, null, "#0044ff", "#ffee00", 4));

        BufferedImage rendered = ImageIO.read(new ByteArrayInputStream(image.content()));
        // The corner is inside the quiet zone, so it is always the backdrop.
        assertThat(rendered.getRGB(0, 0) & 0xFFFFFF).isEqualTo(0xFFEE00);
        assertThat(hasPixel(rendered, 0x0044FF)).isTrue();
    }

    @Test
    void aTransparentBackgroundLeavesTheBackdropOut() throws Exception {
        QrService.QrImage png = service.generate("transparent",
            QrService.QrOptions.of("png", 128, null, null, "#00000000", null));
        BufferedImage rendered = ImageIO.read(new ByteArrayInputStream(png.content()));
        assertThat(rendered.getRGB(0, 0) >>> 24).isZero();

        QrService.QrImage svg = service.generate("transparent",
            QrService.QrOptions.of("svg", null, null, null, "#00000000", null));
        assertThat(new String(svg.content(), StandardCharsets.UTF_8)).doesNotContain("<rect");
    }

    @Test
    void aPartlyTransparentColorBecomesFillOpacity() {
        QrService.QrImage image = service.generate("half there",
            QrService.QrOptions.of("svg", null, null, "#00000080", null, null));

        assertThat(new String(image.content(), StandardCharsets.UTF_8))
            .contains("fill=\"#000000\" fill-opacity=\"0.502\"");
    }

    // endregion

    // region generate — rejected input

    @Test
    void blankDataIsRejected() {
        assertThatThrownBy(() -> service.generate("", options("png")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("data is required");
    }

    @Test
    void aPayloadLongerThanAQrCodeCanHoldIsRejected() {
        String tooLong = "x".repeat(QrService.MAX_DATA_BYTES + 1);

        assertThatThrownBy(() -> service.generate(tooLong, options("png")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("too long");
    }

    @Test
    void aPayloadThatOnlyFitsAtALowerLevelIsRejectedWithTheLevelNamed() {
        // Comfortably inside the absolute ceiling, far past what H can hold.
        String payload = "x".repeat(2000);

        assertThatThrownBy(() -> service.generate(payload,
            QrService.QrOptions.of("png", null, "H", null, null, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("error correction level H");
    }

    @Test
    void badOptionsAreRejected() {
        assertThatThrownBy(() -> QrService.QrOptions.of("bmp", null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported format");

        assertThatThrownBy(() -> QrService.QrOptions.of(null, 16, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be between");

        assertThatThrownBy(() -> QrService.QrOptions.of(null, QrService.MAX_SIZE + 1, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be between");

        assertThatThrownBy(() -> QrService.QrOptions.of(null, null, "Z", null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported errorCorrection");

        assertThatThrownBy(() -> QrService.QrOptions.of(null, null, null, "red", null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("foreground must be a hex color");

        assertThatThrownBy(() -> QrService.QrOptions.of(null, null, null, null, "#12345", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("background must be a hex color");

        assertThatThrownBy(() -> QrService.QrOptions.of(null, null, null, null, null, QrService.MAX_MARGIN + 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("margin must be between");
    }

    @Test
    void defaultsFillInWhenNothingIsAsked() {
        QrService.QrOptions defaults = QrService.QrOptions.of(null, null, null, null, null, null);

        assertThat(defaults.format()).isEqualTo(QrFormat.PNG);
        assertThat(defaults.size()).isEqualTo(QrService.DEFAULT_SIZE);
        assertThat(defaults.errorCorrection()).isEqualTo(QrErrorCorrection.M);
        assertThat(defaults.margin()).isEqualTo(QrService.DEFAULT_MARGIN);
        assertThat(defaults.foreground()).isEqualTo(0xFF000000);
        assertThat(defaults.background()).isEqualTo(0xFFFFFFFF);
    }

    @Test
    void colorShorthandExpandsLikeCss() {
        assertThat(QrService.parseColor("#fff", 0, "c")).isEqualTo(0xFFFFFFFF);
        assertThat(QrService.parseColor("0af", 0, "c")).isEqualTo(0xFF00AAFF);
        assertThat(QrService.parseColor("#00AAFF", 0, "c")).isEqualTo(0xFF00AAFF);
        assertThat(QrService.parseColor("#00aaff80", 0, "c")).isEqualTo(0x8000AAFF);
        assertThat(QrService.parseColor("#0af8", 0, "c")).isEqualTo(0x8800AAFF);
        assertThat(QrService.parseColor("  ", 0x12345678, "c")).isEqualTo(0x12345678);
    }

    // endregion

    // region read

    @Test
    void aCodePrintedLightOnDarkIsStillRead() {
        QrService.QrImage inverted = service.generate("light on dark",
            QrService.QrOptions.of("png", null, null, "#ffffff", "#000000", null));

        QrService.QrRead read = service.read(inverted.content());

        assertThat(read.found()).isTrue();
        assertThat(read.text()).isEqualTo("light on dark");
    }

    @Test
    void anImageWithoutACodeIsAnAnswerNotAnError() throws Exception {
        QrService.QrRead read = service.read(blankPng());

        assertThat(read.found()).isFalse();
        assertThat(read.text()).isNull();
        assertThat(read.format()).isNull();
    }

    @Test
    void bytesThatAreNotAnImageAreRejected() {
        assertThatThrownBy(() -> service.read("not an image at all".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unsupported image format");

        assertThatThrownBy(() -> service.read(new byte[0]))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("an image is required");
    }

    // endregion

    // region helpers

    private String svgOf(String payload, Integer margin) {
        return new String(service.generate(payload,
            QrService.QrOptions.of("svg", null, null, null, null, margin)).content(),
            StandardCharsets.UTF_8);
    }

    private static int viewBoxWidth(String svg) {
        Matcher matcher = Pattern.compile("viewBox=\"0 0 (\\d+) (\\d+)\"").matcher(svg);
        assertThat(matcher.find()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    /**
     * Draws the SVG's module runs into a PNG, so the generated geometry can be
     * decoded rather than merely eyeballed. Only the shapes this service emits
     * are understood — runs of {@code M x y h w v1 h-w z}.
     */
    private static byte[] rasterize(String svg) {
        int modules = viewBoxWidth(svg);
        int scale = 8;

        BufferedImage image = new BufferedImage(modules * scale, modules * scale, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);

        Matcher runs = Pattern.compile("M(\\d+) (\\d+)h(\\d+)v1h-\\d+z").matcher(svg);
        while (runs.find()) {
            int x = Integer.parseInt(runs.group(1));
            int y = Integer.parseInt(runs.group(2));
            int length = Integer.parseInt(runs.group(3));
            graphics.fillRect(x * scale, y * scale, length * scale, scale);
        }
        graphics.dispose();

        return toPng(image);
    }

    private static byte[] blankPng() {
        BufferedImage image = new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 120, 120);
        graphics.dispose();
        return toPng(image);
    }

    private static byte[] toPng(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "PNG", out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private static boolean hasPixel(BufferedImage image, int rgb) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == rgb) {
                    return true;
                }
            }
        }
        return false;
    }

    // endregion
}
