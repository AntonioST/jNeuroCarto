package io.ast.jneurocarto.atlas;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class ImageVolume {
    public final int page;
    public final int height;
    public final int width;
    public final int type;
    private final int[] data;

    public ImageVolume(int page, int height, int width, int type, int[] data) {
        this.page = page;
        this.height = height;
        this.width = width;
        this.type = type;
        this.data = data;
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

    private int index(int page, int height, int width) {
        return page * (this.height * this.width) + height * this.width + width;
    }

    public int get(int page, int height, int width) {
        return data[index(page, height, width)];
    }

    public int get(BrainAtlas.CoordinateIndex coor) {
        return data[index(coor.ap(), coor.dv(), coor.ml())];
    }

    public void set(int page, int height, int width, int value) {
        data[index(page, height, width)] = value;
    }

    public void set(BrainAtlas.CoordinateIndex coor, int value) {
        data[index(coor.ap(), coor.dv(), coor.ml())] = value;
    }

    public BufferedImage image(int page) {
        var ret = new BufferedImage(width, height, type);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ret.setRGB(x, y, data[index(page, y, x)]);
            }
        }
        return ret;
    }

}
