package com.zkt.backend.media;

import com.zkt.backend.common.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

/** Produces the small JPEG shown in lists and grids so clients do not download multi-megabyte originals. */
@Slf4j
@Component
public class ThumbnailGenerator {
    static final int MAX_SIDE = 720;
    // Rejects decompression bombs (e.g. a tiny PNG declaring 60000x60000) before any pixel is decoded.
    static final long MAX_PIXELS = 150_000_000L;
    private static final float JPEG_QUALITY = 0.82f;

    public ThumbnailGenerator() {
        // Registers the WebP reader even when the registry was first initialised by another class loader.
        ImageIO.scanForPlugins();
    }

    /** Returns a JPEG at most {@link #MAX_SIDE} pixels on its longer side, or empty when the image cannot be decoded. */
    public Optional<byte[]> create(byte[] source) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return Optional.empty();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if ((long) width * height > MAX_PIXELS) throw DomainException.badRequest("IMAGE_TOO_LARGE", "图片分辨率过大，请压缩后再上传");
                ImageReadParam param = reader.getDefaultReadParam();
                // Decode at roughly twice the target size so large photos are never fully materialised in memory.
                int step = Math.max(1, Math.max(width, height) / (MAX_SIDE * 2));
                param.setSourceSubsampling(step, step, 0, 0);
                BufferedImage scaled = scale(reader.read(0, param));
                return Optional.of(encode(orient(scaled, ExifOrientation.read(source))));
            } finally {
                reader.dispose();
            }
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Thumbnail generation skipped errorType={}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static BufferedImage scale(BufferedImage image) {
        double ratio = Math.min(1.0, (double) MAX_SIDE / Math.max(image.getWidth(), image.getHeight()));
        int width = Math.max(1, (int) Math.round(image.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(image.getHeight() * ratio));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE); // transparent PNG/WebP areas become white instead of black in JPEG
            g.fillRect(0, 0, width, height);
            g.drawImage(image, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /** Applies EXIF orientation, since re-encoding drops the tag that browsers would otherwise honour. */
    static BufferedImage orient(BufferedImage image, int orientation) {
        int w = image.getWidth(), h = image.getHeight();
        AffineTransform t = switch (orientation) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);   // mirror horizontal
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);  // rotate 180
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);   // mirror vertical
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);    // transpose
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);   // rotate 90 clockwise
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);  // transverse
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);   // rotate 270 clockwise
            default -> null;
        };
        if (t == null) return image;
        boolean swap = orientation >= 5;
        BufferedImage target = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try { g.drawImage(image, t, null); } finally { g.dispose(); }
        return target;
    }

    private static byte[] encode(BufferedImage image) throws java.io.IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    /** Minimal reader for the EXIF Orientation tag (0x0112) in a JPEG's APP1 segment; returns 1 when absent. */
    static final class ExifOrientation {
        private ExifOrientation() {}

        static int read(byte[] b) {
            if (b.length < 4 || (b[0] & 0xff) != 0xFF || (b[1] & 0xff) != 0xD8) return 1;
            int i = 2;
            while (i + 4 <= b.length) {
                if ((b[i] & 0xff) != 0xFF) return 1;
                int marker = b[i + 1] & 0xff;
                if (marker == 0xFF) { i++; continue; }                       // fill byte
                if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD8)) { i += 2; continue; } // standalone markers
                if (marker == 0xDA || marker == 0xD9) return 1;             // image data begins: no EXIF before it
                int length = u16(b, i + 2, true);
                if (length < 2) return 1;
                int start = i + 4, end = Math.min(b.length, i + 2 + length);
                if (marker == 0xE1 && start + 6 <= end && "Exif\0\0".equals(new String(b, start, 6, StandardCharsets.ISO_8859_1)))
                    return fromTiff(b, start + 6, end);
                i += 2 + length;
            }
            return 1;
        }

        private static int fromTiff(byte[] b, int tiff, int end) {
            if (tiff + 8 > end) return 1;
            boolean bigEndian;
            if (b[tiff] == 'M' && b[tiff + 1] == 'M') bigEndian = true;
            else if (b[tiff] == 'I' && b[tiff + 1] == 'I') bigEndian = false;
            else return 1;
            long ifd = u32(b, tiff + 4, bigEndian);
            long entries = tiff + ifd;
            if (ifd < 8 || entries + 2 > end) return 1;
            int count = u16(b, (int) entries, bigEndian);
            for (int n = 0; n < count; n++) {
                long entry = entries + 2 + 12L * n;
                if (entry + 12 > end) return 1;
                if (u16(b, (int) entry, bigEndian) == 0x0112) {
                    int value = u16(b, (int) entry + 8, bigEndian);
                    return value >= 1 && value <= 8 ? value : 1;
                }
            }
            return 1;
        }

        private static int u16(byte[] b, int at, boolean bigEndian) {
            int hi = b[at] & 0xff, lo = b[at + 1] & 0xff;
            return bigEndian ? (hi << 8) | lo : (lo << 8) | hi;
        }

        private static long u32(byte[] b, int at, boolean bigEndian) {
            long value = 0;
            for (int k = 0; k < 4; k++) value = (value << 8) | (b[at + (bigEndian ? k : 3 - k)] & 0xff);
            return value;
        }
    }
}
