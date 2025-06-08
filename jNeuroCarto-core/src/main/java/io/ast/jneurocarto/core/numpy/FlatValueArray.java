package io.ast.jneurocarto.core.numpy;

abstract class FlatValueArray<T> extends ValueArray<T> {
    int[] shape;
    int total;

    FlatValueArray(char valueType, int valeSize) {
        super(valueType, valeSize);
    }

    FlatValueArray(char valueType, int valueSize, String acceptValueTypes) {
        super(valueType, valueSize, acceptValueTypes);
    }

    final void checkShape(NumpyHeader header) {
        shape = header.shape();
        if (shape.length > 0) {
            var total = 1L;
            for (int j : shape) total *= j;
            if (total >= Integer.MAX_VALUE) {
                throw new RuntimeException("over jvm limitation");
            }
            this.total = (int) total;
        } else {
            this.total = 0;
        }

        checkValue(header);
    }

    final void checkFor(int[] shape, int total) {
        this.shape = shape;
        if (shape.length > 0) {
            var t = 1L;
            for (int j : shape) t *= j;
            if (t >= Integer.MAX_VALUE) {
                throw new RuntimeException("over jvm limitation");
            }
            this.total = (int) t;
        } else {
            this.total = 0;
        }
        if (this.total != total) throw new RuntimeException();
    }

    @Override
    public final int[] shape() {
        return shape;
    }
}
