package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

/**
 * @param <T> target array type.
 */
public abstract class ValueArray<T> {

    protected char valueType = '\0';
    protected int valueSize = 0;

    protected ValueArray() {
    }

    protected ValueArray(char valueType, int valueSize) {
        this.valueType = valueType;
        this.valueSize = valueSize;
    }

    protected final void checkValue(NumpyHeader header) {
        var descr = header.descr();
        var valueType = descr.charAt(1);
        valueSize = Integer.parseInt(descr.substring(2));
        if (valueType != this.valueType) {
            throw new UnsupportedNumpyDataFormatException(header, "not an " + this.valueType + " array, but : " + descr);
        }
        if (valueSize != 1 && valueSize != 2 && valueSize != 4 && valueSize != 8) {
            throw new UnsupportedNumpyDataFormatException(header, "not an " + this.valueType + " array, but : " + descr);
        }
    }

    public abstract int[] shape();

    public final String descr() {
        return (valueSize == 1 ? "|" : "<") + valueType + valueSize;
    }

    /**
     * create the array instance before reading.
     *
     * @param header
     * @return
     */
    protected abstract T create(NumpyHeader header);

    /**
     * check the array instance and initialize itself before writing.
     *
     * @param data
     */
    protected abstract void checkFor(T data);

    /**
     * read a value from buffer.
     *
     * @param ret
     * @param pos
     * @param buffer
     * @return successful
     */
    protected abstract boolean read(T ret, long pos, ByteBuffer buffer);

    /**
     * write a value into buffer.
     *
     * @param ret
     * @param pos
     * @param buffer
     * @return successful
     */
    protected abstract boolean write(T ret, long pos, ByteBuffer buffer);

    protected final int readInt(ByteBuffer buffer) {
        return switch (valueSize) {
            case 1 -> buffer.get();
            case 2 -> buffer.getShort();
            case 4 -> buffer.getInt();
            case 8 -> (int) buffer.getLong();
            default -> throw new RuntimeException();
        };
    }

    protected final void writeInt(ByteBuffer buffer, int value) {
        switch (valueSize) {
        case 1 -> buffer.put((byte) value);
        case 2 -> buffer.putShort((short) value);
        case 4 -> buffer.putInt(value);
        case 8 -> buffer.putLong(value);
        default -> throw new RuntimeException();
        }
    }

    protected final double readDouble(ByteBuffer buffer) {
        return buffer.getDouble();
    }

    protected final void writeDouble(ByteBuffer buffer, double value) {
        buffer.putDouble(value);
    }
}
