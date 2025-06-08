package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfD3Double extends D3Array<double[][][]> {
    OfD3Double() {
        super('f', 8, "if");
    }

    @Override
    public double[][][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new double[plans][][];
        for (int p = 0; p < plans; p++) {
            var row = ret[p] = new double[rows][];
            for (int r = 0; r < rows; r++) {
                row[r] = new double[columns];
            }
        }
        return ret;
    }

    @Override
    public void checkFor(double[][][] data) {
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
    public boolean read(double[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            double v;
            if (valueType == 'f') {
                v = readDouble(buffer);
            } else {
                v = readInt(buffer);
            }
            ret[p][r][c] = v;
            return true;
        }
        return false;
    }

    @Override
    public boolean write(double[][][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            writeDouble(buffer, ret[p][r][c]);
            return true;
        }
        return false;
    }
}
