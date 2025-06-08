package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfBoolean extends D1Array<boolean[]> {
    OfBoolean() {
        super('b', 1);
    }

    @Override
    public boolean[] create(NumpyHeader header) {
        return new boolean[checkShape(header)];
    }

    @Override
    public void checkFor(boolean[] data) {
        checkFor(data.length);
    }

    @Override
    public boolean read(boolean[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            ret[p] = readInt(buffer) > 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean write(boolean[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            writeInt(buffer, ret[p] ? 1 : 0);
            return true;
        }
        return false;
    }
}
