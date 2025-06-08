package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfFlatBoolean extends FlatValueArray<FlatBooleanArray> {
    OfFlatBoolean() {
        super('b', 1);
    }

    @Override
    protected FlatBooleanArray create(NumpyHeader header) {
        checkShape(header);
        return new FlatBooleanArray(shape, new boolean[total]);
    }

    @Override
    protected void checkFor(FlatBooleanArray data) {
        checkFor(data.shape(), data.array().length);
    }

    @Override
    protected boolean read(FlatBooleanArray ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < total) {
            ret.array()[p] = readInt(buffer) > 0;
            return true;
        }
        return false;
    }

    @Override
    protected boolean write(FlatBooleanArray ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < total) {
            writeInt(buffer, ret.array()[p] ? 1 : 0);
            return true;
        }
        return false;
    }
}
