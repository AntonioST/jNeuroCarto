package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfD2Int extends D2Array<int[][]> {
    OfD2Int() {
        super('i', 4, false);
    }

    OfD2Int(boolean columnFirst) {
        super('i', 4, columnFirst);
    }

    @Override
    public int[][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new int[length1][length2];
        for (int i = 0; i < length1; i++) ret[i] = new int[length2];
        return ret;
    }

    @Override
    public void checkFor(int[][] data) {
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
    public boolean read(int[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            ret[index1][index2] = readInt(buffer);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(int[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            writeInt(buffer, ret[index1][index2]);
            return true;
        }
        return false;
    }
}
