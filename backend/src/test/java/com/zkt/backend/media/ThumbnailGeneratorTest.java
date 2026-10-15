package com.zkt.backend.media;

import com.zkt.backend.common.DomainException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThumbnailGeneratorTest {
    private final ThumbnailGenerator generator = new ThumbnailGenerator();

    @Test void downscalesLargeImagesKeepingAspectRatio() throws Exception {
        BufferedImage thumb = decode(generator.create(encode(halves(3000, 1500), "png")).orElseThrow());
        assertThat(thumb.getWidth()).isEqualTo(720);
        assertThat(thumb.getHeight()).isEqualTo(360);
    }

    @Test void neverUpscalesSmallImages() throws Exception {
        BufferedImage thumb = decode(generator.create(encode(halves(100, 50), "png")).orElseThrow());
        assertThat(thumb.getWidth()).isEqualTo(100);
        assertThat(thumb.getHeight()).isEqualTo(50);
    }

    @Test void appliesExifOrientationSoPortraitPhotosStayUpright() throws Exception {
        // Landscape pixels, left half red / right half blue, tagged "rotate 90° clockwise" like a phone held upright.
        byte[] jpeg = withOrientation(encode(halves(80, 40), "jpg"), 6, true);
        BufferedImage thumb = decode(generator.create(jpeg).orElseThrow());
        assertThat(thumb.getWidth()).isEqualTo(40);
        assertThat(thumb.getHeight()).isEqualTo(80);
        assertThat(isRed(thumb.getRGB(20, 10))).as("left half rotates to the top").isTrue();
        assertThat(isRed(thumb.getRGB(20, 70))).isFalse();
    }

    @Test void readsOrientationInBothByteOrders() throws Exception {
        byte[] jpeg = encode(halves(8, 8), "jpg");
        assertThat(ThumbnailGenerator.ExifOrientation.read(jpeg)).isEqualTo(1);
        assertThat(ThumbnailGenerator.ExifOrientation.read(withOrientation(jpeg, 8, true))).isEqualTo(8);
        assertThat(ThumbnailGenerator.ExifOrientation.read(withOrientation(jpeg, 3, false))).isEqualTo(3);
    }

    @Test void decodesWebp() throws Exception {
        byte[] webp = Base64.getDecoder().decode("UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoBAAEAAwA0JaQAA3AA/vuUAAA=");
        BufferedImage thumb = decode(generator.create(webp).orElseThrow());
        assertThat(thumb.getWidth()).isEqualTo(1);
    }

    @Test void rejectsDecompressionBombsBeforeDecoding() {
        assertThatThrownBy(() -> generator.create(pngHeader(60_000, 60_000)))
                .isInstanceOf(DomainException.class).hasMessageContaining("分辨率过大");
    }

    @Test void undecodableImagesFallBackToNoThumbnail() {
        assertThat(generator.create(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1})).isEmpty();
    }

    private static BufferedImage halves(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED); g.fillRect(0, 0, width / 2, height);
        g.setColor(Color.BLUE); g.fillRect(width / 2, 0, width - width / 2, height);
        g.dispose();
        return image;
    }

    private static boolean isRed(int rgb) {
        int r = (rgb >> 16) & 0xff, b = rgb & 0xff;
        return r > 180 && b < 80;
    }

    private static byte[] encode(BufferedImage image, String format) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws Exception { return ImageIO.read(new ByteArrayInputStream(bytes)); }

    /** Inserts an APP1 EXIF segment holding only the Orientation tag right after the JPEG SOI marker. */
    static byte[] withOrientation(byte[] jpeg, int orientation, boolean bigEndian) {
        ByteBuffer tiff = ByteBuffer.allocate(26).order(bigEndian ? java.nio.ByteOrder.BIG_ENDIAN : java.nio.ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte) (bigEndian ? 'M' : 'I')).put((byte) (bigEndian ? 'M' : 'I')).putShort((short) 42).putInt(8)
                .putShort((short) 1)                                   // one IFD entry
                .putShort((short) 0x0112).putShort((short) 3).putInt(1) // Orientation, SHORT, count 1
                .putShort((short) orientation).putShort((short) 0)
                .putInt(0);                                            // no next IFD
        byte[] exifHeader = "Exif\0\0".getBytes(StandardCharsets.ISO_8859_1);
        int length = 2 + exifHeader.length + tiff.capacity();
        ByteBuffer out = ByteBuffer.allocate(jpeg.length + 2 + length);
        out.put(jpeg, 0, 2).put((byte) 0xFF).put((byte) 0xE1).putShort((short) length).put(exifHeader).put(tiff.array()).put(jpeg, 2, jpeg.length - 2);
        return out.array();
    }

    private static byte[] pngHeader(int width, int height) {
        ByteBuffer ihdr = ByteBuffer.allocate(17).put("IHDR".getBytes(StandardCharsets.US_ASCII))
                .putInt(width).putInt(height).put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0);
        CRC32 crc = new CRC32(); crc.update(ihdr.array());
        return ByteBuffer.allocate(8 + 4 + 17 + 4 + 12)
                .put(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})
                .putInt(13).put(ihdr.array()).putInt((int) crc.getValue())
                .putInt(0).put("IEND".getBytes(StandardCharsets.US_ASCII)).putInt(0xAE426082).array();
    }
}
