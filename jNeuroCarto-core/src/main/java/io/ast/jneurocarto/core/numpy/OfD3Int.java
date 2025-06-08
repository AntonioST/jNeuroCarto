package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfD3Int extends D3Array<int[][][]> {
    OfD3Int() {
        super('i', 4);
    }

    @Override
    public int[][][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new int[plans][][];
        for (int p = 0; p < plans; p++) {
            var row = ret[p] = new int[rows][];
            for (int r = 0; r < rows; r++) {
                row[r] = new int[columns];
            }
        }
        return ret;
    }

    @Override
    public void checkFor(int[][][] data) {
        var plans = data.length;
        var rows = plans == 0 ? 0 : data[0].length;
        var columns = plans == 0 || rows == 0 ? 0 : data[0][0].length;
        for (int p = 1; p < plans; p++) {
            var row = data[p];
            if (row.length != rows) {
                throw new IllegalArgumentException("not an array[%d][%d][%d]".formatted(plans, rows, columns));
            }
            for (int r = 0; r < rows; r++) {
                if (row[r].length != columns) {
                    throw new IllegalArgumentException("not an array[%d][%d][%d]".formatted(plans, rows, columns));
                }
            }
        }
        checkFor(plans, rows, columns);
    }

    @Override
    public boolean read(int[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            ret[p][r][c] = readInt(buffer);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(int[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            writeInt(buffer, ret[p][r][c]);
            return true;
        }
        return false;
    }
}
