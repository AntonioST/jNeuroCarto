package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;

abstract class D2Array<T> extends ValueArray<T> {

    final boolean columnFirst;
    int rows;
    int columns;
    int length1;
    int length2;
    int index1;
    int index2;

    D2Array(char valueType, int valeSize, boolean columnFirst) {
        super(valueType, valeSize);
        this.columnFirst = columnFirst;
    }

    D2Array(char valueType, int valueSize, String acceptValueTypes, boolean columnFirst) {
        super(valueType, valueSize, acceptValueTypes);
        this.columnFirst = columnFirst;
    }

    final void checkShape(NumpyHeader header) {
        var shape = header.shape();

        if (shape.length != 2) throw new RuntimeException("not an 2-d array : " + Arrays.toString(shape));
        rows = shape[0];
        columns = shape[1];

        if (columnFirst) {
            length1 = columns;
            length2 = rows;
        } else {
            length1 = rows;
            length2 = columns;
        }

        checkValue(header);
    }

    @Override
    public final int[] shape() {
        return new int[]{rows, columns};
    }

    final void checkFor(int length1, int length2) {
        this.length1 = length1;
        this.length2 = length2;
        if (columnFirst) {
            columns = length1;
            rows = length2;
        } else {
            rows = length1;
            columns = length2;
        }
    }

    final boolean index(long pos) {
        if (columnFirst) {
            index1 = (int) (pos % columns);
            index2 = (int) (pos / columns);
        } else {
            index1 = (int) (pos / columns);
            index2 = (int) (pos % columns);
        }
        return index1 < length1 && index2 < length2;
    }
}
