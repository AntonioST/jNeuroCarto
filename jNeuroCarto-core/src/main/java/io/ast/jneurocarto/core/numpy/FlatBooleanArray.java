package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;

public record FlatBooleanArray(int[] shape, boolean[] array) implements FlatArray {

    public FlatBooleanArray {
        var size = 1;
        for (var i : shape) {
            if (i < 0) throw new IllegalArgumentException("negative axis : " + Arrays.toString(shape));
            size *= i;
        }
        if (array.length != size) throw new IllegalArgumentException("array size mis-match tp shape");
    }

    public FlatBooleanArray(int... shape) {
        this(shape, new boolean[(int) NumpyHeader.size(shape)]);
    }

    public int size() {
        return array.length;
    }

    public boolean get(int... index) {
        return array[index(index)];
    }

    public void set(boolean value, int... index) {
        array[index(index)] = value;
    }
}
