package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfD2Double extends D2Array<double[][]> {
    OfD2Double() {
        super('f', 8, "if", false);
    }

    OfD2Double(boolean columnFirst) {
        super('f', 8, columnFirst);
    }

    @Override
    public double[][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new double[length1][length2];
        for (int i = 0; i < length1; i++) ret[i] = new double[length2];
        return ret;
    }

    @Override
    public void checkFor(double[][] data) {
        var length1 = data.length;
        var length2 = data[0].length;
        for (int i = 1; i < length1; i++) {
            if (data[i].length != length2) {
                throw new IllegalArgumentException("not an array[%d][%d]".formatted(length1, length2));
            }
        }
        checkFor(length1, length2);
    }

    @Override
    public boolean read(double[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            double v;
            if (valueType == 'f') {
                v = readDouble(buffer);
            } else {
                v = readInt(buffer);
            }
            ret[index1][index2] = v;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean write(double[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            writeDouble(buffer, ret[index1][index2]);
            return true;
        } else {
            return false;
        }
    }
}
