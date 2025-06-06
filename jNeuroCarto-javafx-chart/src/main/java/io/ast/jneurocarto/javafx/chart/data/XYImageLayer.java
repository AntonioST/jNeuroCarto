package io.ast.jneurocarto.javafx.chart.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Effect;
import javafx.scene.image.Image;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.Nullable;


public class XYImageLayer implements XYGraphics {

    protected double z = 0;
    protected @Nullable BlendMode blend = null;
    protected @Nullable Effect effect = null;
    protected boolean visible = true;

    protected List<XYImage> images = new ArrayList<>();

    @Override
    public int size() {
        return images.size();
    }

    @Override
    public double z() {
        return z;
    }

    public void z(double z) {
        this.z = z;
    }

    public @Nullable BlendMode blend() {
        return blend;
    }

    public void blend(@Nullable BlendMode blend) {
        this.blend = blend;
    }

    public @Nullable Effect effect() {
        return effect;
    }

    public void effect(@Nullable Effect effect) {
        this.effect = effect;
    }


    @Override
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /*========*
     * Images *
     *========*/

    public Stream<XYImage> images() {
        return images.stream();
    }

    public void clearImage() {
        images.clear();
    }

    public void addImage(XYImage image) {
        if (image.layer == this) {
            throw new RuntimeException("image has beed added.");
        } else if (image.layer != null) {
            throw new RuntimeException("image has belong to another layer.");
        }

        image.layer = this;
        images.add(image);
        images.sort(Comparator.comparing(XYImage::z));
    }

    public void removeImage(XYImage image) {
        if (image.layer == this) {
            image.layer = null;
            images.remove(image);
        }
    }

    void dirtyZ() {
        images.sort(Comparator.comparing(XYImage::z));
    }

    /*================*
     * transformation *
     *================*/

    @Override
    public int transform(Affine aff, double[][] p) {
        return 0;
    }

    /*==========*
     * plotting *
     *==========*/

    @Override
    public void paint(GraphicsContext gc) {
        if (!isVisible() || images.isEmpty()) return;

        gc.save();
        try {
            gc.setGlobalBlendMode(blend);
            gc.setEffect(effect);

            for (var image : images) {
                image.paint(gc);
            }
        } finally {
            gc.restore();
        }
    }

    @Override
    public void paint(GraphicsContext gc, double[][] p, int offset, int length) {
        paint(gc);
    }

    /*=========*
     * Builder *
     *=========*/

    public Builder builder() {
        return new Builder(this);
    }

    public static class Builder {
        private final XYImageLayer layer;

        public Builder(XYImageLayer layer) {
            this.layer = layer;
        }

        public XYImageLayer layer() {
            return layer;
        }

        public int size() {
            return layer.size();
        }

        public Builder z(double z) {
            layer.z(z);
            return this;
        }

        public Builder blend(BlendMode blend) {
            layer.blend(blend);
            return this;
        }

        public Builder effect(Effect effect) {
            layer.effect(effect);
            return this;
        }

        public Builder setVisible(boolean visible) {
            layer.setVisible(visible);
            return this;
        }

        public XYImage addImage(Path imageFile) throws IOException {
            try (var in = new BufferedInputStream(Files.newInputStream(imageFile))) {
                return addImage(new Image(in));
            }
        }

        public XYImage addImage(Image image) {
            var ret = new XYImage(image);
            layer.addImage(ret);
            return ret;
        }

        public XYImage addImage(Image image, double x, double y) {
            var ret = new XYImage(image, x, y);
            layer.addImage(ret);
            return ret;
        }

        public XYImage addImage(Image image, double x, double y, double w, double h) {
            var ret = new XYImage(image, x, y, w, h);
            layer.addImage(ret);
            return ret;
        }
    }
}
