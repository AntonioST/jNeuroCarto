package io.ast.jneurocarto.atlas;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class ImageVolume {
    public final int page;
    public final int height;
    public final int width;
    private final int[] data;

    public ImageVolume(int page, int height, int width, int type, int[] data) {
        this.page = page;
        this.height = height;
        this.width = width;
        this.data = data;
    }

    public ImageVolume(ImageVolume image) {
        this.page = image.page;
        this.height = image.height;
        this.width = image.width;
        this.data = image.data.clone();
    }

    /// load tiff image.
    ///
    /// [reference](https://github.com/haraldk/TwelveMonkeys?tab=readme-ov-file#advanced-usage)
    public static ImageVolume readTiff(Path file) throws IOException {
        try (var input = ImageIO.createImageInputStream(file.toFile())) {
            var readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException("No reader for: " + file);
            }

            var reader = readers.next();
            try {
                reader.setInput(input);

                int page = reader.getNumImages(true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int type = reader.getRawImageType(0).getBufferedImageType();
                var data = new int[page * height * width];
                var ret = new ImageVolume(page, height, width, type, data);

                for (int p = 0; p < page; p++) {
                    var image = reader.read(p);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            ret.set(p, y, x, image.getRGB(x, y));
                        }
                    }
                }

                return ret;
            } finally {
                reader.dispose();
            }
        }
    }

    public int[] shape() {
        return new int[]{page, height, width};
    }

    private int index(int page, int y, int x) {
        return page * (height * width) + y * width + x;
    }

    public int get(int page, int y, int x) {
        try {
            return data[index(page, y, x)];
        } catch (IndexOutOfBoundsException e) {
            throw newIAE(page, y, x, e);
        }
    }

    public int get(BrainAtlas.CoordinateIndex coor) {
        try {
            return data[index(coor.ap(), coor.dv(), coor.ml())];
        } catch (IndexOutOfBoundsException e) {
            throw newIAE(coor.ap(), coor.dv(), coor.ml(), e);
        }
    }

    public void set(int page, int y, int x, int value) {
        try {
            data[index(page, y, x)] = value;
        } catch (IndexOutOfBoundsException e) {
            throw newIAE(page, y, x, e);
        }
    }

    public void set(BrainAtlas.CoordinateIndex coor, int value) {
        try {
            data[index(coor.ap(), coor.dv(), coor.ml())] = value;
        } catch (IndexOutOfBoundsException e) {
            throw newIAE(coor.ap(), coor.dv(), coor.ml(), e);
        }
    }

    private IllegalArgumentException newIAE(int p, int y, int x, IndexOutOfBoundsException e) {
        return new IllegalArgumentException(String.format("index(page=%d, y=%d, x=%d) out from (%d, %d, %d)", p, y, x, page, height, width), e);
    }

    public void normalizeGrayLevel() {
        normalizeGrayLevel(1);
    }

    public void normalizeGrayLevel(float factor) {
        if (factor <= 0) throw new IllegalArgumentException("factor = " + factor);

        var m = Arrays.stream(data).map(it -> it & 0xFF).max().orElse(0);
        if (m == 0) return;

        var total = page * width * height;
        for (int i = 0; i < total; i++) {
            var value = data[i] & 0xFF;
            value = Math.min((int) ((float) value / m / factor * 0xFF), 0xFF);
            data[i] = 0xFF000000 | (value << 16) | (value << 8) | (value);
        }
    }


    public BufferedImage image(int page) {
        var ret = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ret.setRGB(x, y, data[index(page, y, x)]);
            }
        }
        return ret;
    }

}
