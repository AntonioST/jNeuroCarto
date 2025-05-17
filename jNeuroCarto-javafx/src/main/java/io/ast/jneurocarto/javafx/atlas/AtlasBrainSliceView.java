package io.ast.jneurocarto.javafx.atlas;

import java.util.Objects;

import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class AtlasBrainSliceView extends Canvas {

    private boolean scale = true;

    public final ObjectProperty<SliceCoordinate> anchor = new SimpleObjectProperty<>(null);

    private ImageSlice sliceCache;
    private Image imageCache;

    public AtlasBrainSliceView() {
        super(600, 500);
    }

    public AtlasBrainSliceView(double width, double height) {
        super(width, height);
    }

    public boolean isScale() {
        return scale;
    }

    public void setScale(boolean scale) {
        this.scale = scale;
    }

    public void draw(ImageSlice slice) {
        Image image;
        if (Objects.equals(slice, sliceCache)) {
            image = imageCache;
        } else {
            image = imageCache = slice.imageFx();
            sliceCache = slice;
        }

        var width = getWidth();
        var height = width * image.getHeight() / image.getWidth();
        setHeight(height);

        var gc = getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        if (scale) {
            gc.drawImage(image, 0, 0, width, height);
        } else {
            gc.drawImage(image, 0, 0);
        }

        var anchor = this.anchor.get();
        if (anchor != null) {
            gc.setFill(Color.RED);
            var x = anchor.x();
            var y = anchor.y();
            x = x / slice.width() * width;
            y = y / slice.height() * height;
            gc.fillOval(x - 2, y - 2, 4, 4);
        }
    }
}
