package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;

abstract class D1Array<T> extends ValueArray<T> {

    int length;

    D1Array(char valueType, int valeSize) {
        super(valueType, valeSize);
    }

    D1Array(char valueType, int valueSize, String acceptValueTypes) {
        super(valueType, valueSize, acceptValueTypes);
    }

    final int checkShape(NumpyHeader header) {
        var shape = header.shape();

        if (shape.length != 1) throw new RuntimeException("not an 1-d array : " + Arrays.toString(shape));
        checkValue(header);
        length = shape[0];
        return length;
    }

    @Override
    public final int[] shape() {
        return new int[]{length};
    }

    final void checkFor(int length) {
        this.length = length;
    }

}
