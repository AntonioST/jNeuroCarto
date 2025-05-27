package io.ast.jneurocarto.javafx.atlas;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.javafx.chart.ImagePainter;

@NullMarked
public class SlicePainter extends ImagePainter {

    private @Nullable ImageSlice sliceCache;

    public void update(ImageSlice slice) {
        if (!Objects.equals(slice, sliceCache)) {
            sliceCache = slice;
            width(slice.width());
            height(slice.height());
            setImage(slice.imageFx());
        }
    }
}
