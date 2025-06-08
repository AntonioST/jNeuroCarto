package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfFlatDouble extends FlatValueArray<FlatDoubleArray> {
    OfFlatDouble() {
        super('f', 8, "if");
    }

    @Override
    protected FlatDoubleArray create(NumpyHeader header) {
        checkShape(header);
        return new FlatDoubleArray(shape, new double[total]);
    }

    @Override
    protected void checkFor(FlatDoubleArray data) {
        checkFor(data.shape(), data.array().length);
    }

    @Override
    protected boolean read(FlatDoubleArray ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < total) {
            double v;
            if (valueType == 'f') {
                v = readDouble(buffer);
            } else {
                v = readInt(buffer);
            }
            ret.array()[p] = v;
            return true;
        }
        return false;
    }

    @Override
    protected boolean write(FlatDoubleArray ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < total) {
            writeDouble(buffer, ret.array()[p]);
            return true;
        }
        return false;
    }
}
