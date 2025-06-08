package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class OfString extends D1Array<String[]> {

    private char[] buffer;

    public OfString() {
        super('U', 0);
    }

    @Override
    protected String[] create(NumpyHeader header) {
        checkShape(header);
        buffer = new char[valueSize];
        return new String[length];
    }

    @Override
    protected void checkValue(NumpyHeader header) {
        valueSize = checkStringValue(header);
    }

    static int checkStringValue(NumpyHeader header) {
        var descr = header.descr();
        var valueType = descr.charAt(1);
        var valueSize = Integer.parseInt(descr.substring(2));
        if (valueType != 'U') {
            throw new UnsupportedNumpyDataFormatException(header, "not an U array, but : " + descr);
        }
        return valueSize;
    }

    @Override
    protected void checkFor(String[] data) {
        checkFor(data.length);
        valueSize = maxStringLength(data);
    }

    static int maxStringLength(String[] data) {
        return Arrays.stream(data)
            .mapToInt(String::length)
            .max()
            .orElse(0);
    }

    @Override
    protected boolean read(String[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            ret[p] = read(this.buffer, buffer);
            return true;
        }
        return false;
    }

    static String read(char[] chars, ByteBuffer buffer) {
        for (int i = 0, length = chars.length; i < length; i++) {
            chars[i] = (char) buffer.getInt();
        }
        return new String(chars);
    }

    @Override
    protected boolean write(String[] ret, long pos, ByteBuffer buffer) {
        var p = (int) pos;
        if (p < ret.length) {
            write(ret[p], valueSize, buffer);
            return true;
        }
        return false;
    }

    static void write(String str, int length, ByteBuffer buffer) {
        for (int i = 0; i < length; i++) {
            if (i < str.length()) {
                buffer.putInt(str.charAt(i));
            } else {
                buffer.putInt(0);
            }
        }
    }
}
