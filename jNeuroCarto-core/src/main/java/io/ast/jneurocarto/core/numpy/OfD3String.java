package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class OfD3String extends D3Array<String[][][]> {
    private char[] buffer;

    OfD3String() {
        super('U', 0);
    }

    @Override
    public String[][][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new String[plans][][];
        for (int p = 0; p < plans; p++) {
            var row = ret[p] = new String[rows][];
            for (int r = 0; r < rows; r++) {
                row[r] = new String[columns];
            }
        }
        buffer = new char[valueSize];
        return ret;
    }

    @Override
    protected void checkValue(NumpyHeader header) {
        valueSize = OfString.checkStringValue(header);
    }

    @Override
    public void checkFor(String[][][] data) {
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
        valueSize = maxStringLength(data);
    }

    static int maxStringLength(String[][][] data) {
        return Arrays.stream(data)
            .mapToInt(OfD2String::maxStringLength)
            .max()
            .orElse(0);
    }

    @Override
    public boolean read(String[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            ret[p][r][c] = OfString.read(this.buffer, buffer);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(String[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            OfString.write(ret[p][r][c], valueSize, buffer);
            return true;
        }
        return false;
    }
}
