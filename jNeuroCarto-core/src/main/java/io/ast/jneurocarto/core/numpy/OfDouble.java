package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfDouble extends D1Array<double[]> {
    OfDouble() {
        super('f', 8, "if");
    }

    @Override
    public double[] create(NumpyHeader header) {
        return new double[checkShape(header)];
    }

    @Override
    public void checkFor(double[] data) {
        checkFor(data.length);
    }

    @Override
    public boolean read(double[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            double v;
            if (valueType == 'f') {
                v = readDouble(buffer);
            } else {
                v = readInt(buffer);
            }
            ret[p] = v;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean write(double[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            writeDouble(buffer, ret[p]);
            return true;
        } else {
            return false;
        }
    }
}
