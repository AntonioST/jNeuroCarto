package io.ast.jneurocarto.probe_npx.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;

import static java.nio.file.StandardOpenOption.*;

/// [Numpy format](https://numpy.org/devdocs/reference/generated/numpy.lib.format.html)
///
/// ```
/// struct header{
///     u6 MAGIC;
///     u1 majorVersion;
///     u1 minorVersion;
///     u2< length;
///     u[length] data;
///}
///```
///
/// Example:
/// ```
/// 00000000: 934e 554d 5059 0100 7600 7b27 6465 7363  .NUMPY..v.{'desc
/// 00000010: 7227 3a20 273c 6938 272c 2027 666f 7274  r': '<i8', 'fort
/// 00000020: 7261 6e5f 6f72 6465 7227 3a20 4661 6c73  ran_order': Fals
/// 00000030: 652c 2027 7368 6170 6527 3a20 2835 3132  e, 'shape': (512
/// 00000040: 302c 2035 292c 207d 2020 2020 2020 2020  0, 5),}
/// 00000050: 2020 2020 2020 2020 2020 2020 2020 2020
/// 00000060: 2020 2020 2020 2020 2020 2020 2020 2020
/// 00000070: 2020 2020 2020 2020 2020 2020 2020 200a                 .
///```
///
/// TODO any library?
@NullMarked
public final class Numpy {

    public static final byte[] MAGIC = {(byte) 0x93, 'N', 'U', 'M', 'P', 'Y'};

    private Numpy() {
        throw new RuntimeException();
    }

    record NumpyHeader(
      int majorVersion,
      int minorVersion,
      String data
    ) {
        NumpyHeader {
            data = data.strip();
            if (!(data.startsWith("{") && data.endsWith("}"))) {
                throw new IllegalArgumentException("not a dict string : " + data);
            }
        }

        public static NumpyHeader of(int majorVersion, int minorVersion, String descr, boolean fortranOrder, int[] shape) {
            var data = "{'descr': '" +
                       descr +
                       "', 'fortran_order': " +
                       (fortranOrder ? "True" : "False") +
                       ", 'shape': (" +
                       Arrays.stream(shape).mapToObj(Integer::toString).collect(Collectors.joining(", ")) +
                       "), }";
            return new NumpyHeader(majorVersion, minorVersion, data);
        }

        public String descr() {
            var i = indexOf("descr");
            if (i < 0) throw new IllegalArgumentException("descr key not found");
            var j = data.indexOf("'", i + 1);
            if (j < 0) throw new IllegalArgumentException("descr value not found");
            var k = data.indexOf("'", j + 1);
            if (k < 0) throw new IllegalArgumentException("descr value not found");
            return data.substring(j + 1, k);
        }

        public boolean fortranOrder() {
            var i = indexOf("fortran_order");
            if (i < 0) throw new IllegalArgumentException("fortran_order key not found");
            var j = data.indexOf(",", i + 1);
            if (j < 0) throw new IllegalArgumentException("fortran_order value not found");
            return Boolean.parseBoolean(data.substring(i + 1, j).strip().toLowerCase());
        }

        public int[] shape() {
            var i = indexOf("shape");
            if (i < 0) throw new IllegalArgumentException("shape key not found");
            var j = data.indexOf("(", i + 1);
            if (j < 0) throw new IllegalArgumentException("shape value not found");
            var k = data.indexOf(")", j + 1);
            if (k < 0) throw new IllegalArgumentException("shape value not found");
            return Arrays.stream(data.substring(j + 1, k).split(", +"))
              .mapToInt(Integer::parseInt)
              .toArray();
        }

        private int indexOf(String name) {
            var key = "'" + name + "':";
            var ret = data.indexOf(key);
            if (ret < 0) return -1;
            return ret + key.length();
        }
    }

    public sealed interface ArrayCreator<T> permits ValueArray {
        T create(NumpyHeader header);

        void checkFor(T data);

        int[] shape();

        String descr();

        int valueSize();

        /**
         * read value from buffer.
         *
         * @param ret
         * @param pos
         * @param buffer
         * @return successful
         */
        boolean read(T ret, long pos, ByteBuffer buffer);

        /**
         * write value into buffer.
         *
         * @param ret
         * @param pos
         * @param buffer
         * @return successful
         */
        boolean write(T ret, long pos, ByteBuffer buffer);

    }

    private static sealed abstract class ValueArray<T> implements ArrayCreator<T> permits D1Array, D2Array, D3Array {

        final char valueType;
        int valueSize = 0;

        ValueArray(char valueType) {
            this.valueType = valueType;
            valueSize = switch (valueType) {
                case 'i' -> 4;
                case 'f' -> 8;
                default -> 0;
            };
        }

        @Override
        public final int valueSize() {
            return valueSize;
        }

        final void checkValue(NumpyHeader header) {
            var descr = header.descr();
            var valueType = descr.charAt(1);
            valueSize = Integer.parseInt(descr.substring(2));
            if (valueType != this.valueType) throw new RuntimeException("not an " + this.valueType + " array, but : " + descr);
            if (valueSize != 1 && valueSize != 2 && valueSize != 4 && valueSize != 8)
                throw new RuntimeException("not an " + this.valueType + " array, but : " + descr);
        }

        @Override
        public final String descr() {
            return "<" + valueType + valueSize;
        }

        final int readInt(ByteBuffer buffer) {
            return switch (valueSize) {
                case 1 -> buffer.get();
                case 2 -> buffer.getShort();
                case 4 -> buffer.getInt();
                case 8 -> (int) buffer.getLong();
                default -> throw new RuntimeException();
            };
        }

        final void writeInt(ByteBuffer buffer, int value) {
            switch (valueSize) {
            case 1 -> buffer.put((byte) value);
            case 2 -> buffer.putShort((short) value);
            case 4 -> buffer.putInt(value);
            case 8 -> buffer.putLong(value);
            default -> throw new RuntimeException();
            }
        }

        final double readDouble(ByteBuffer buffer) {
            return buffer.getDouble();
        }

        final void writeDouble(ByteBuffer buffer, double value) {
            buffer.putDouble(value);
        }
    }

    private static abstract sealed class D1Array<T> extends ValueArray<T> permits OfInt, OfDouble {

        int length;

        D1Array(char valueType) {
            super(valueType);
        }

        final int checkShape(NumpyHeader header) {
            var shape = header.shape();

            if (shape.length != 1) throw new RuntimeException("not an 1-d array : " + Arrays.toString(shape));
            checkValue(header);
            length = shape[0];
            return length;
        }

        @Override
        public final int[] shape() {
            return new int[]{length};
        }

        final void checkFor(int length) {
            this.length = length;
        }

    }

    private static sealed abstract class D2Array<T> extends ValueArray<T> permits OfD2Int, OfD2Double {

        final boolean columnFirst;
        int rows;
        int columns;
        int length1;
        int length2;
        int index1;
        int index2;

        D2Array(char valueType, boolean columnFirst) {
            super(valueType);
            this.columnFirst = columnFirst;
        }

        final void checkShape(NumpyHeader header) {
            var shape = header.shape();

            if (shape.length != 2) throw new RuntimeException("not an 2-d array : " + Arrays.toString(shape));
            rows = shape[0];
            columns = shape[1];

            if (columnFirst) {
                length1 = columns;
                length2 = rows;
            } else {
                length1 = rows;
                length2 = columns;
            }

            checkValue(header);
        }

        @Override
        public final int[] shape() {
            return new int[]{rows, columns};
        }

        final void checkFor(int length1, int length2) {
            this.length1 = length1;
            this.length2 = length2;
            if (columnFirst) {
                columns = length1;
                rows = length2;
            } else {
                rows = length1;
                columns = length2;
            }
        }

        final boolean index(long pos) {
            if (columnFirst) {
                index1 = (int) (pos % columns);
                index2 = (int) (pos / columns);
            } else {
                index1 = (int) (pos / columns);
                index2 = (int) (pos % columns);
            }
            return index1 < length1 && index2 < length2;
        }
    }

    private static abstract sealed class D3Array<T> extends ValueArray<T> permits OfD3Int, OfD3Double {
        int plans;
        int rows;
        int columns;
        int p;
        int r;
        int c;

        D3Array(char valueType) {
            super(valueType);
        }

        final void checkShape(NumpyHeader header) {
            var shape = header.shape();
            if (shape.length != 3) throw new RuntimeException("not an 3-d array : " + Arrays.toString(shape));
            plans = shape[0];
            rows = shape[1];
            columns = shape[2];
            checkValue(header);
        }


        @Override
        public final int[] shape() {
            return new int[]{plans, rows, columns};
        }

        final void checkFor(int plans, int rows, int columns) {
            this.plans = plans;
            this.rows = rows;
            this.columns = columns;
        }

        final boolean index(long pos) {
            var rc = rows * columns;
            p = (int) (pos / rc);
            var q = (int) (pos % rc);
            rows = q / c;
            columns = q % c;
            return p < plans && r < rows && c < columns;
        }
    }

    public static final class OfInt extends D1Array<int[]> {
        public OfInt() {
            super('i');
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

    public static final class OfD2Int extends D2Array<int[][]> {
        public OfD2Int() {
            super('i', false);
        }

        public OfD2Int(boolean columnFirst) {
            super('i', columnFirst);
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
                if (data[i].length != length2) throw new IllegalArgumentException("not an array[%d][%d]".formatted(length1, length2));
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

    public static final class OfD3Int extends D3Array<int[][][]> {
        public OfD3Int() {
            super('i');
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
                if (row.length != rows) throw new IllegalArgumentException("not an array[%d][%d][%d]".formatted(plans, rows, columns));
                for (int r = 0; r < rows; r++) {
                    if (row[r].length != columns) throw new IllegalArgumentException("not an array[%d][%d][%d]".formatted(plans, rows, columns));
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

    public static final class OfDouble extends D1Array<double[]> {
        public OfDouble() {
            super('f');
        }

        @Override
        public double[] create(NumpyHeader header) {
            return new double[checkShape(header)];
        }

        @Override
        public void checkFor(double[] data) {
            checkFor(data.length);
        }

        @Override
        public boolean read(double[] ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < ret.length) {
                ret[p] = readDouble(buffer);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean write(double[] ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < ret.length) {
                writeDouble(buffer, ret[p]);
                return true;
            } else {
                return false;
            }
        }
    }

    public static final class OfD2Double extends D2Array<double[][]> {
        public OfD2Double() {
            super('f', false);
        }

        public OfD2Double(boolean columnFirst) {
            super('f', columnFirst);
        }

        @Override
        public double[][] create(NumpyHeader header) {
            checkShape(header);
            var ret = new double[length1][length2];
            for (int i = 0; i < length1; i++) ret[i] = new double[length2];
            return ret;
        }

        @Override
        public void checkFor(double[][] data) {
            var length1 = data.length;
            var length2 = data[0].length;
            for (int i = 1; i < length1; i++) {
                if (data[i].length != length2) throw new IllegalArgumentException("not an array[%d][%d]".formatted(length1, length2));
            }
            checkFor(length1, length2);
        }

        @Override
        public boolean read(double[][] ret, long pos, ByteBuffer buffer) {
            if (index(pos)) {
                ret[index1][index2] = readDouble(buffer);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean write(double[][] ret, long pos, ByteBuffer buffer) {
            if (index(pos)) {
                writeDouble(buffer, ret[index1][index2]);
                return true;
            } else {
                return false;
            }
        }
    }

    public static final class OfD3Double extends D3Array<double[][][]> {
        public OfD3Double() {
            super('f');
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
                if (row.length != rows) throw new IllegalArgumentException("not an array[%d][%d][%d]".formatted(plans, rows, columns));
                for (int r = 0; r < rows; r++) {
                    if (row[r].length != columns) throw new IllegalArgumentException("not an array[%d][%d][%d]".formatted(plans, rows, columns));
                }
            }
            checkFor(plans, rows, columns);
        }

        @Override
        public boolean read(double[][][] ret, long pos, ByteBuffer buffer) {
            if (index(pos)) {
                ret[p][r][c] = readDouble(buffer);
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

    /**
     * read numpy array from file.
     *
     * @param file .npy file
     * @param of   array create handler
     * @return
     * @throws IOException
     */
    public static <T> T read(Path file, ArrayCreator<T> of) throws IOException {
        try (var channel = Files.newByteChannel(file)) {
            return read(channel, of);
        }
    }

    /**
     * read numpy array from stream.
     *
     * @param in
     * @param of array create handler
     * @return
     * @throws IOException
     */
    public static <T> T read(InputStream in, ArrayCreator<T> of) throws IOException {
        return read(Channels.newChannel(in), of);
    }

    /**
     * @param channel
     * @return int[C][R] array
     * @throws IOException
     */
    private static <T> T read(ReadableByteChannel channel, ArrayCreator<T> of) throws IOException {
        var header = readHeader(channel);

        if (header.fortranOrder()) throw new IOException("not an C-array");

        T ret = of.create(header);

        var descr = header.descr();
        boolean isLittle = descr.charAt(0) == '<';

        var valueSize = of.valueSize();
        var buffer = ByteBuffer.allocate(32 * 8 * valueSize);
        buffer.order(isLittle ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        long pos = 0L;
        while (true) {
            buffer.clear();
            if (channel.read(buffer) < 0) break;
            buffer.flip();
            while (buffer.remaining() >= valueSize) {
                of.read(ret, pos++, buffer);
            }
        }

        return ret;
    }


    /**
     * write numpy array to file.
     *
     * @param file
     * @param array
     * @throws IOException
     */
    public static void write(Path file, int[] array) throws IOException {
        var of = new OfInt();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, int[][] array) throws IOException {
        var of = new OfD2Int();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, int[][][] array) throws IOException {
        var of = new OfD3Int();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, double[] array) throws IOException {
        var of = new OfDouble();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, double[][] array) throws IOException {
        var of = new OfD2Double();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, double[][][] array) throws IOException {
        var of = new OfD3Double();
        of.checkFor(array);
        write(file, array, of);
    }

    public static <T> void write(Path file, T array, ArrayCreator<T> of) throws IOException {
        of.checkFor(array);
        try (var channel = Files.newByteChannel(file, CREATE, TRUNCATE_EXISTING, WRITE)) {
            write(channel, array, of);
        }
    }

    /**
     * write numpy array to stream.
     *
     * @param out
     * @param array
     * @throws IOException
     */
    public static void write(OutputStream out, int[] array) throws IOException {
        var of = new OfInt();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, int[][] array) throws IOException {
        var of = new OfD2Int();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, int[][][] array) throws IOException {
        var of = new OfD3Int();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, double[] array) throws IOException {
        var of = new OfDouble();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, double[][] array) throws IOException {
        var of = new OfD2Double();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, double[][][] array) throws IOException {
        var of = new OfD3Double();
        of.checkFor(array);
        write(out, array, of);
    }

    public static <T> void write(OutputStream out, T array, ArrayCreator<T> of) throws IOException {
        of.checkFor(array);
        write(Channels.newChannel(out), array, of);
    }

    private static <T> void write(WritableByteChannel channel, T array, ArrayCreator<T> of) throws IOException {
        writeHeader(channel, NumpyHeader.of(1, 0, of.descr(), false, of.shape()));

        var valueSize = of.valueSize();
        var buffer = ByteBuffer.allocate(32 * 8 * valueSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        long pos = 0L;
        while (of.write(array, pos++, buffer)) {

            if (buffer.remaining() == 0) {
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
            }
        }

        if (buffer.position() > 0) {
            buffer.flip();
            channel.write(buffer);
            buffer.clear();
        }
    }

    static NumpyHeader readHeader(ReadableByteChannel channel) throws IOException {
        var buffer = ByteBuffer.allocate(64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        channel.read(buffer);
        buffer.flip();

        for (int i = 0; i < MAGIC.length; i++) {
            var read = buffer.get();
            if (read != MAGIC[i]) throw new IOException("not a npy file : [" + i + "]=" + read);
        }

        byte majorVersion = buffer.get();
        byte minorVersion = buffer.get();
        int length = buffer.getShort();

        byte[] rawData = new byte[length];
        int put = buffer.remaining();
        buffer.get(rawData, 0, buffer.remaining());

        while (put < length) {
            buffer.clear();
            buffer.limit(Math.min(length - put, buffer.capacity()));
            if (channel.read(buffer) < 0) throw new IOException("EOF");
            buffer.flip();

            var remain = buffer.remaining();
            buffer.get(rawData, put, remain);
            put += remain;
        }

        var data = new String(rawData);
        if (!data.endsWith("\n")) {
            throw new IOException("illegal padding char in header : \"" + data + "\"");
        }

        return new NumpyHeader(majorVersion, minorVersion, data);
    }

    static void writeHeader(WritableByteChannel channel, NumpyHeader header) throws IOException {
        var buffer = ByteBuffer.allocate(64);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(MAGIC);
        buffer.put((byte) header.majorVersion);
        buffer.put((byte) header.minorVersion);

        var data = header.data.getBytes();
        int length = buffer.position() + 2 + data.length;
        length = ((length / 64) + 1) * 64 - buffer.position() - 2;
        buffer.putShort((short) length);

        var put = buffer.remaining();
        buffer.put(data, 0, Math.min(data.length, put));

        while (put < data.length) {
            assert buffer.remaining() == 0;
            buffer.flip();
            channel.write(buffer);
            buffer.clear();

            var remain = Math.min(64, data.length - put);
            buffer.put(data, put, remain);
            put += remain;
        }

        while (buffer.remaining() > 1) {
            buffer.put((byte) ' ');
        }
        buffer.put((byte) '\n');
        assert buffer.remaining() == 0;
        buffer.flip();
        channel.write(buffer);
    }
}
