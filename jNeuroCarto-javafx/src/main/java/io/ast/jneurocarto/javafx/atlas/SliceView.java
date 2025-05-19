package io.ast.jneurocarto.javafx.atlas;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;

import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.atlas.SliceCoordinate;

public class SliceView extends Canvas {

    public final ObjectProperty<SliceCoordinate> anchor = new SimpleObjectProperty<>(null);

    public final SlicePainter painter = new SlicePainter();

    public SliceView() {
        super(600, 500);
    }

    public SliceView(double width, double height) {
        super(width, height);
    }

    public void draw(ImageSlice slice) {
        var width = getWidth();
        var height = width * slice.heightPx() / slice.widthPx();
        setHeight(height);

        painter.s(width / slice.widthPx());

        var gc = getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        painter.update(slice);
        painter.draw(gc);

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
