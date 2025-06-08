package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfD2Boolean extends D2Array<boolean[][]> {
    OfD2Boolean() {
        super('b', 1, false);
    }

    OfD2Boolean(boolean columnFirst) {
        super('b', 1, columnFirst);
    }

    @Override
    public boolean[][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new boolean[length1][length2];
        for (int i = 0; i < length1; i++) ret[i] = new boolean[length2];
        return ret;
    }

    @Override
    public void checkFor(boolean[][] data) {
        var length1 = data.length;
        var length2 = length1 == 0 ? 0 : data[0].length;
        for (int i = 1; i < length1; i++) {
            if (data[i].length != length2) {
                throw new IllegalArgumentException("not an array[%d][%d]".formatted(length1, length2));
            }
        }
        checkFor(length1, length2);
    }

    @Override
    public boolean read(boolean[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            ret[index1][index2] = readInt(buffer) > 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean write(boolean[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            writeInt(buffer, ret[index1][index2] ? 1 : 0);
            return true;
        }
        return false;
    }
}
