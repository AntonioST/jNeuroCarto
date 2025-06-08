package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class OfD2String extends D2Array<String[][]> {

    private char[] buffer;

    OfD2String() {
        super('U', 0, false);
    }

    OfD2String(boolean columnFirst) {
        super('U', 0, columnFirst);
    }

    @Override
    public String[][] create(NumpyHeader header) {
        checkShape(header);
        var ret = new String[length1][length2];
        for (int i = 0; i < length1; i++) ret[i] = new String[length2];
        buffer = new char[valueSize];
        return ret;
    }

    @Override
    protected void checkValue(NumpyHeader header) {
        valueSize = OfString.checkStringValue(header);
    }

    @Override
    public void checkFor(String[][] data) {
        var length1 = data.length;
        var length2 = length1 == 0 ? 0 : data[0].length;
        for (int i = 1; i < length1; i++) {
            if (data[i].length != length2) {
                throw new IllegalArgumentException("not an array[%d][%d]".formatted(length1, length2));
            }
        }
        checkFor(length1, length2);
        valueSize = maxStringLength(data);
    }

    static int maxStringLength(String[][] data) {
        return Arrays.stream(data)
            .mapToInt(OfString::maxStringLength)
            .max()
            .orElse(0);
    }

    @Override
    public boolean read(String[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            ret[index1][index2] = OfString.read(this.buffer, buffer);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(String[][] ret, long pos, ByteBuffer buffer) {
        if (index(pos)) {
            OfString.write(ret[index1][index2], valueSize, buffer);
            return true;
        }
        return false;
    }
}
