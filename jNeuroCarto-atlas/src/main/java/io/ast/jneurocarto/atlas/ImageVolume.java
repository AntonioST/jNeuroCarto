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

    /**
     * Is the value stored in {@link #data} is RGBA color?
     */
    public final boolean colored;

    private final int[] data;

    public ImageVolume(int page, int height, int width, boolean colored) {
        if (page < 0) throw new IllegalArgumentException("negative page : " + page);
        if (height < 0) throw new IllegalArgumentException("negative height : " + height);
        if (width < 0) throw new IllegalArgumentException("negative width : " + width);
        this.page = page;
        this.height = height;
        this.width = width;
        this.colored = colored;
        this.data = new int[page * height * width];
    }

    public ImageVolume(ImageVolume image) {
        this.page = image.page;
        this.height = image.height;
        this.width = image.width;
        this.colored = image.colored;
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
                var ret = new ImageVolume(page, height, width, type != BufferedImage.TYPE_CUSTOM);

                if (!ret.colored) {
                    var buffer = new int[width];
                    for (int p = 0; p < page; p++) {
                        var raster = reader.read(p).getRaster();
                        for (int y = 0; y < height; y++) {
                            raster.getPixels(0, y, width, 1, buffer);
                            ret.set(p, 0, y, width, 1, buffer);
                        }
                    }
                } else {
                    for (int p = 0; p < page; p++) {
                        var image = reader.read(p);
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                ret.set(p, x, y, image.getRGB(x, y));
                            }
                        }
                    }
                }

                return ret;
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * {@return int array of {page, height, width}}
     */
    public int[] shape() {
        return new int[]{page, height, width};
    }

    private int index(int page, int y, int x) {
        if (page < 0 || page >= this.page) throw new IndexOutOfBoundsException("page (%d) over boundary (%d).".formatted(page, this.page));
        if (x < 0 || x >= width) throw new IndexOutOfBoundsException("x (%d) over width (%d).".formatted(x, width));
        if (y < 0 || y >= height) throw new IndexOutOfBoundsException("y (%d) over height (%d).".formatted(y, height));
        return page * (height * width) + y * width + x;
    }

    public int get(int page, int x, int y) {
        return data[index(page, y, x)];
    }

    public int[] get(int page, int x, int y, int w, int h, int[] buffer) {
        return get(page, x, y, w, h, buffer, 0);
    }

    public int[] get(int page, int x, int y, int w, int h, int[] buffer, int offset) {
        if (w == 0 || h == 0) return buffer;
        if (w < 0 || h < 0) throw new IllegalArgumentException("negative width or height");
        if (offset + w * h > buffer.length) {
            throw new IndexOutOfBoundsException("buffer too small");
        }

        var origin = index(page, y, x); // check and throw
        index(page, y + h - 1, x + w - 1); // check and throw
        for (int j = 0; j < h; j++) {
            System.arraycopy(data, origin + j * width, buffer, offset + j * w, w);
        }
        return buffer;
    }

    public int get(BrainAtlas.CoordinateIndex coor) {
        return data[index(coor.ap(), coor.dv(), coor.ml())];
    }

    public void set(int page, int x, int y, int value) {
        data[index(page, y, x)] = value;
    }

    public void set(int page, int x, int y, int w, int h, int[] buffer) {
        set(page, x, y, w, h, buffer, 0);
    }

    public void set(int page, int x, int y, int w, int h, int[] buffer, int offset) {
        if (w == 0 || h == 0) return;
        if (w < 0 || h < 0) throw new IllegalArgumentException("negative width or height");
        if (offset + w * h > buffer.length) {
            throw new IndexOutOfBoundsException("buffer too small");
        }

        var origin = index(page, y, x); // check and throw
        index(page, y + h - 1, x + w - 1); // check and throw

        for (int j = 0; j < h; j++) {
            System.arraycopy(buffer, offset + j * w, data, origin + j * width, w);
        }
    }

    public void set(BrainAtlas.CoordinateIndex coor, int value) {
        data[index(coor.ap(), coor.dv(), coor.ml())] = value;
    }

    public void normalizeGrayLevel() {
        normalizeGrayLevel(1);
    }

    public void normalizeGrayLevel(float factor) {
        if (!colored) throw new IllegalArgumentException("not a colored value.");
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
        if (!colored) throw new IllegalArgumentException("not a colored image.");
        var ret = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ret.setRGB(x, y, data[index(page, y, x)]);
            }
        }
        return ret;
    }

}
