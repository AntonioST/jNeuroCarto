package io.ast.jneurocarto.atlas.gui;

import io.ast.jneurocarto.atlas.ImageSlice;
import javafx.scene.canvas.Canvas;

public class AtlasBrainSliceView extends Canvas {

    private boolean scale = true;

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
        var gc = getGraphicsContext2D();
        var image = slice.imageFx();

        var width = getWidth();
        var height = width * image.getHeight() / image.getWidth();
        setHeight(height);

        gc.clearRect(0, 0, width, height);

        if (scale) {
            gc.drawImage(image, 0, 0, width, height);
        } else {
            gc.drawImage(image, 0, 0);
        }
    }
}
