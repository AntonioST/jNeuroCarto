package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfD3Boolean extends D3Array<boolean[][][]> {
    OfD3Boolean() {
        super('i', 4);
    }

    @Override
    public boolean[][][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new boolean[plans][][];
        for (int p = 0; p < plans; p++) {
            var row = ret[p] = new boolean[rows][];
            for (int r = 0; r < rows; r++) {
                row[r] = new boolean[columns];
            }
        }
        return ret;
    }

    @Override
    public void checkFor(boolean[][][] data) {
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
    public boolean read(boolean[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            ret[p][r][c] = readInt(buffer) > 0;
            return true;
        }
        return false;
    }

    @Override
    public boolean write(boolean[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            writeInt(buffer, ret[p][r][c] ? 1 : 0);
            return true;
        }
        return false;
    }
}
