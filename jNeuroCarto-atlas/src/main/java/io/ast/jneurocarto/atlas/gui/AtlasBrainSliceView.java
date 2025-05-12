package io.ast.jneurocarto.atlas.gui;

import io.ast.jneurocarto.atlas.ImageSlice;
import javafx.scene.canvas.Canvas;

public class AtlasBrainSliceView extends Canvas {

    public AtlasBrainSliceView() {
        super(500, 400);
    }

    public AtlasBrainSliceView(double width, double height) {
        super(width, height);
    }

    public void draw(ImageSlice image) {
        var gc = getGraphicsContext2D();
        gc.drawImage(image.imageFx(), 0, 0);
    }
}
