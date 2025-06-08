package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;

abstract class D3Array<T> extends ValueArray<T> {
    int plans;
    int rows;
    int columns;
    int p;
    int r;
    int c;

    D3Array(char valueType, int valeSize) {
        super(valueType, valeSize);
    }

    D3Array(char valueType, int valueSize, String acceptValueTypes) {
        super(valueType, valueSize, acceptValueTypes);
    }

    final void checkShape(NumpyHeader header) {
        var shape = header.shape();
        if (shape.length != 3) throw new RuntimeException("not an 3-d array : " + Arrays.toString(shape));
        plans = shape[0];
        rows = shape[1];
        columns = shape[2];
        checkValue(header);
    }

    @Override
    public final int[] shape() {
        return new int[]{plans, rows, columns};
    }

    final void checkFor(int plans, int rows, int columns) {
        this.plans = plans;
        this.rows = rows;
        this.columns = columns;
    }

    final boolean index(long pos) {
        var rc = rows * columns;
        p = (int) (pos / rc);
        var q = (int) (pos % rc);
        rows = q / c;
        columns = q % c;
        return p < plans && r < rows && c < columns;
    }
}
