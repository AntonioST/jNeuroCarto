package io.ast.jneurocarto.javafx.chart.data;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.transform.Affine;

import org.jspecify.annotations.NullMarked;

import io.ast.jneurocarto.javafx.chart.InteractionXYPainter;

@NullMarked
public interface XYGraphics {

    /**
     * {@return number of data}
     */
    int size();

    /**
     * number of data points. It is used by {@link InteractionXYPainter} to determinate
     * the size of points need to be transformed for pre-allocating the array
     * (used for parameter {@code p} in {@link #transform(Affine, double[][])}
     * and {@link #paint(GraphicsContext, double[][], int, int)}).
     * If a {@link XYGraphics} does not need to calculate the transform point,
     * it could return 0 to avoid unnecessary array allocation.
     *
     * @return number of data points
     */
    default int points() {
        return size();
    }

    double z();

    boolean isVisible();

    default void paint(GraphicsContext gc) {
        if (!isVisible()) return;

        var p = createTransformedArray(size());

        var length = transform(gc.getTransform(), p);
        paint(gc, p, 0, length);
    }

    /**
     * transform the data position from chart coordinate into canvas coordinate,
     * and store the result into {@code p}.
     *
     * @param aff {@link GraphicsContext}'s affine transformation.
     * @param p   {@code double[4][row]} array that store the transformed data.
     * @return number of row used.
     */
    int transform(Affine aff, double[][] p);

    /**
     * @param gc
     * @param p      {@code double[4][row]} array that store the transformed data.
     * @param offset the beginning row of the data.
     * @param length number of data.
     */
    void paint(GraphicsContext gc, double[][] p, int offset, int length);

    /**
     * @param length
     * @return {@code double[4][length]} array
     */
    static double[][] createTransformedArray(int length) {
        var ret = new double[4][];
        ret[0] = new double[length];
        ret[1] = new double[length];
        ret[2] = new double[length];
        ret[3] = new double[length];
        return ret;
    }
}
