package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfFlatInt extends FlatValueArray<FlatIntArray> {
    OfFlatInt() {
        super('i', 4);
    }

    @Override
    protected FlatIntArray create(NumpyHeader header) {
        checkShape(header);
        return new FlatIntArray(shape, new int[total]);
    }

    @Override
    protected void checkFor(FlatIntArray data) {
        checkFor(data.shape(), data.array().length);
    }

    @Override
    protected boolean read(FlatIntArray ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < total) {
            ret.array()[p] = readInt(buffer);
            return true;
        }
        return false;
    }

    @Override
    protected boolean write(FlatIntArray ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < total) {
            writeInt(buffer, ret.array()[p]);
            return true;
        }
        return false;
    }
}
