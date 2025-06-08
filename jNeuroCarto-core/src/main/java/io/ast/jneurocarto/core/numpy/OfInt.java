package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfInt extends D1Array<int[]> {
    OfInt() {
        super('i', 4);
    }

    @Override
    public int[] create(NumpyHeader header) {
        return new int[checkShape(header)];
    }

    @Override
    public void checkFor(int[] data) {
        checkFor(data.length);
    }

    @Override
    public boolean read(int[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            ret[p] = readInt(buffer);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(int[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            writeInt(buffer, ret[p]);
            return true;
        }
        return false;
    }
}
