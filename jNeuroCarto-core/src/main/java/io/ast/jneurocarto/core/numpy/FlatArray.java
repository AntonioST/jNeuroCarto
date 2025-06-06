package io.ast.jneurocarto.core.numpy;

public sealed interface FlatArray permits FlatIntArray, FlatDoubleArray {

    int[] shape();

    default int ndim() {
        return shape().length;
    }

    default int size() {
        return (int) NumpyHeader.size(shape());
    }

    default int index(int... index) {
        var shape = shape();
        var ndim = shape.length;
        if (ndim != index.length) throw new IllegalArgumentException();

        return switch (ndim) {
            case 0 -> -1;
            case 1 -> checkBoundary(index[0], shape[0]);
            case 2 -> checkBoundary(index[0], shape[0]) * shape[1] + checkBoundary(index[1], shape[1]);
            case 3 -> checkBoundary(index[0], shape[0]) * shape[1] * shape[2]
                      + checkBoundary(index[1], shape[1]) * shape[1]
                      + checkBoundary(index[2], shape[2]);
            default -> {
                var step = 1;
                var ret = 0;
                for (int d = ndim - 1; d >= 0; d--) {
                    var i = index[d];
                    var s = shape[d];
                    ret += step * checkBoundary(i, s);
                    step *= s;
                }
                yield ret;
            }
        };
    }

    private static int checkBoundary(int i, int s) {
        if (!(0 <= i && i < s)) throw new IndexOutOfBoundsException();
        return i;
    }
}
