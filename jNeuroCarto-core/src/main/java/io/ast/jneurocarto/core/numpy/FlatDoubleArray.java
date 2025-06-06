package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;

public record FlatDoubleArray(int[] shape, double[] array) implements FlatArray {

    public FlatDoubleArray {
        var size = 1;
        for (var i : shape) {
            if (i < 0) throw new IllegalArgumentException("negative axis : " + Arrays.toString(shape));
            size *= i;
        }
        if (array.length != size) throw new IllegalArgumentException("array size mis-match tp shape");
    }

    public FlatDoubleArray(int... shape) {
        this(shape, new double[(int) NumpyHeader.size(shape)]);
    }

    public int size() {
        return array.length;
    }

    public double get(int... index) {
        return array[index(index)];
    }

    public void set(double value, int... index) {
        array[index(index)] = value;
    }
}
