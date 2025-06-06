package io.ast.jneurocarto.core.numpy;

import java.util.Arrays;

public record FlatIntArray(int[] shape, int[] array) implements FlatArray {

    public FlatIntArray {
        var size = 1;
        for (var i : shape) {
            if (i < 0) throw new IllegalArgumentException("negative axis : " + Arrays.toString(shape));
            size *= i;
        }
        if (array.length != size) throw new IllegalArgumentException("array size mis-match tp shape");
    }

    public FlatIntArray(int... shape) {
        this(shape, new int[(int) NumpyHeader.size(shape)]);
    }

    public int size() {
        return array.length;
    }

    public int get(int... index) {
        return array[index(index)];
    }

    public void set(int value, int... index) {
        array[index(index)] = value;
    }
}
