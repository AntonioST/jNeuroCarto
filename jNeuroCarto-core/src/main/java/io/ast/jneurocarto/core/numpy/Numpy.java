package io.ast.jneurocarto.core.numpy;

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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static java.nio.file.StandardOpenOption.*;

/// [Numpy format](https://numpy.org/devdocs/reference/generated/numpy.lib.format.html)
///
/// ```h
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
/// ```text
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

    private static abstract class D1Array<T> extends ValueArray<T> {

        int length;

        D1Array(char valueType, int valeSize) {
            super(valueType, valeSize);
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

    private static abstract class D2Array<T> extends ValueArray<T> {

        final boolean columnFirst;
        int rows;
        int columns;
        int length1;
        int length2;
        int index1;
        int index2;

        D2Array(char valueType, int valeSize, boolean columnFirst) {
            super(valueType, valeSize);
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

    private static abstract class D3Array<T> extends ValueArray<T> {
        int plans;
        int rows;
        int columns;
        int p;
        int r;
        int c;

        D3Array(char valueType, int valeSize) {
            super(valueType, valeSize);
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

    private static abstract class FlattenArray<T> extends ValueArray<T> {
        int[] shape;
        int total;

        FlattenArray(char valueType, int valeSize) {
            super(valueType, valeSize);
        }

        final void checkShape(NumpyHeader header) {
            shape = header.shape();
            if (shape.length > 0) {
                var total = 1L;
                for (int j : shape) total *= j;
                if (total >= Integer.MAX_VALUE) {
                    throw new RuntimeException("over jvm limitation");
                }
                this.total = (int) total;
            } else {
                this.total = 0;
            }

            checkValue(header);
        }

        final void checkFor(int[] shape, int total) {
            this.shape = shape;
            if (shape.length > 0) {
                var t = 1L;
                for (int j : shape) t *= j;
                if (t >= Integer.MAX_VALUE) {
                    throw new RuntimeException("over jvm limitation");
                }
                this.total = (int) t;
            } else {
                this.total = 0;
            }
            if (this.total != total) throw new RuntimeException();
        }

        @Override
        public final int[] shape() {
            return shape;
        }
    }

    public static class OfBuffer extends ValueArray<ByteBuffer> {
        final ByteBuffer buffer;
        int[] shape;

        public OfBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public int[] shape() {
            return shape;
        }

        @Override
        protected ByteBuffer create(NumpyHeader header) {
            this.shape = header.shape();
            return buffer;
        }

        @Override
        protected final void checkFor(ByteBuffer data) {
            if (buffer != data) throw new RuntimeException();
        }

        /**
         * this method has changed its purpose from reading value into
         * a callback of read value.
         * <br/>
         * If you have limited size of {@link #buffer}, make sure transfer the data from it
         * for next coming data.
         * <p>
         * {@snippet :
         * boolean read(ByteBuffer ret, long pos, ByteBuffer buffer) {
         *     ret.flip();
         *     // do something read from ret.
         *     ret.reset();
         *     return true;
         * }
         *}
         *
         * @param ret    {@link #buffer}
         * @param pos    the position of read data.
         * @param buffer always {@code null}.
         * @return successful
         */
        @Override
        protected boolean read(ByteBuffer ret, long pos, @Nullable ByteBuffer buffer) {
            return ret.remaining() > 0;
        }

        /**
         * this method has changed its purpose from writing value into
         * a pre-callback of written value.
         * <br/>
         * If you have limited size of {@link #buffer}, make sure transfer the data into it
         * for next writing action.
         * <p>
         * {@snippet :
         * boolean write(ByteBuffer ret, long pos, ByteBuffer buffer) {
         *     ret.reset();
         *     // do something writing into ret.
         *     ret.flip();
         *     return true;
         * }
         *}
         *
         * @param ret    {@link #buffer}
         * @param pos    the position of written data.
         * @param buffer always {@code null}.
         * @return successful
         */
        @Override
        protected boolean write(ByteBuffer ret, long pos, @Nullable ByteBuffer buffer) {
            return ret.remaining() > 0;
        }
    }

    public static ValueArray<boolean[]> ofBoolean() {
        return new OfBoolean();
    }

    private static final class OfBoolean extends D1Array<boolean[]> {
        private OfBoolean() {
            super('b', 1);
        }

        @Override
        public boolean[] create(NumpyHeader header) {
            return new boolean[checkShape(header)];
        }

        @Override
        public void checkFor(boolean[] data) {
            checkFor(data.length);
        }

        @Override
        public boolean read(boolean[] ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < ret.length) {
                ret[p] = readInt(buffer) > 0;
                return true;
            }
            return false;
        }

        @Override
        public boolean write(boolean[] ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < ret.length) {
                writeInt(buffer, ret[p] ? 1 : 0);
                return true;
            }
            return false;
        }
    }

    public static ValueArray<int[]> ofInt() {
        return new OfInt();
    }

    private static final class OfInt extends D1Array<int[]> {
        private OfInt() {
            super('i', 4);
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

    public static ValueArray<int[][]> ofD2Int() {
        return new OfD2Int();
    }

    public static ValueArray<int[][]> ofD2Int(boolean columnFirst) {
        return new OfD2Int(columnFirst);
    }

    private static final class OfD2Int extends D2Array<int[][]> {
        private OfD2Int() {
            super('i', 4, false);
        }

        private OfD2Int(boolean columnFirst) {
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

    public static ValueArray<int[][][]> ofD3Int() {
        return new OfD3Int();
    }

    private static final class OfD3Int extends D3Array<int[][][]> {
        private OfD3Int() {
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

    public record FlattenIntArray(int[] shape, int[] array) {
    }

    public static ValueArray<FlattenIntArray> ofFlattenInt() {
        return new OfFlattenInt();
    }

    private static final class OfFlattenInt extends FlattenArray<FlattenIntArray> {
        private OfFlattenInt() {
            super('i', 4);
        }

        @Override
        protected FlattenIntArray create(NumpyHeader header) {
            checkShape(header);
            return new FlattenIntArray(shape, new int[total]);
        }

        @Override
        protected void checkFor(FlattenIntArray data) {
            checkFor(data.shape, data.array.length);
        }

        @Override
        protected boolean read(FlattenIntArray ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < total) {
                ret.array[p] = readInt(buffer);
                return true;
            }
            return false;
        }

        @Override
        protected boolean write(FlattenIntArray ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < total) {
                writeInt(buffer, ret.array[p]);
                return true;
            }
            return false;
        }
    }

    public static ValueArray<double[]> ofDouble() {
        return new OfDouble();
    }

    private static final class OfDouble extends D1Array<double[]> {
        private OfDouble() {
            super('f', 8);
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

    public static ValueArray<double[][]> ofD2Double() {
        return new OfD2Double();
    }

    public static ValueArray<double[][]> ofD2Double(boolean columnFirst) {
        return new OfD2Double(columnFirst);
    }

    private static final class OfD2Double extends D2Array<double[][]> {
        private OfD2Double() {
            super('f', 8, false);
        }

        private OfD2Double(boolean columnFirst) {
            super('f', 8, columnFirst);
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
                if (data[i].length != length2) {
                    throw new IllegalArgumentException("not an array[%d][%d]".formatted(length1, length2));
                }
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

    public static ValueArray<double[][][]> ofD3Double() {
        return new OfD3Double();
    }

    private static final class OfD3Double extends D3Array<double[][][]> {
        private OfD3Double() {
            super('f', 8);
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


    public record FlattenDoubleArray(int[] shape, double[] array) {
    }

    public static ValueArray<FlattenDoubleArray> ofFlattenDouble() {
        return new OfFlattenDouble();
    }

    private static final class OfFlattenDouble extends FlattenArray<FlattenDoubleArray> {
        private OfFlattenDouble() {
            super('f', 8);
        }

        @Override
        protected FlattenDoubleArray create(NumpyHeader header) {
            checkShape(header);
            return new FlattenDoubleArray(shape, new double[total]);
        }

        @Override
        protected void checkFor(FlattenDoubleArray data) {
            checkFor(data.shape, data.array.length);
        }

        @Override
        protected boolean read(FlattenDoubleArray ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < total) {
                ret.array[p] = readDouble(buffer);
                return true;
            }
            return false;
        }

        @Override
        protected boolean write(FlattenDoubleArray ret, long pos, ByteBuffer buffer) {
            var p = (int) pos;
            if (p < total) {
                writeDouble(buffer, ret.array[p]);
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
    public static <T> T read(Path file, ValueArray<T> of) throws IOException {
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
    public static <T> T read(InputStream in, ValueArray<T> of) throws IOException {
        return read(Channels.newChannel(in), of);
    }

    /**
     * @param channel
     * @return int[C][R] array
     * @throws IOException
     */
    private static <T> T read(ReadableByteChannel channel, ValueArray<T> of) throws IOException {
        var header = readHeader(channel);

        if (header.fortranOrder()) throw new IOException("not an C-array");

        T ret = of.create(header);

        if (of instanceof OfBuffer buffer) {
            long pos = 0L;
            while (true) {
                var read = channel.read(buffer.buffer);
                if (read < 0) break;
                pos += read;
                if (!buffer.read(buffer.buffer, pos, null)) break;
            }
        } else {
            var descr = header.descr();
            boolean isLittle = descr.charAt(0) == '<';

            var valueSize = of.valueSize;
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
        }

        return ret;
    }

    public interface CheckNumberHeader {
        ValueArray<?> check(NumpyHeader header);
    }

    public record Read(NumpyHeader header, Object data) {
        public int ndim() {
            return header().ndim();
        }

        public int[] shape() {
            return header.shape();
        }

        public String descr() {
            return header.descr();
        }
    }

    public static Read read(Path file, CheckNumberHeader checker) throws IOException {
        try (var channel = Files.newByteChannel(file)) {
            return read(channel, checker);
        }
    }

    public static Read read(InputStream in, CheckNumberHeader checker) throws IOException {
        return read(Channels.newChannel(in), checker);
    }

    private static Read read(ReadableByteChannel channel, CheckNumberHeader checker) throws IOException {
        var header = readHeader(channel);

        if (header.fortranOrder()) throw new UnsupportedNumpyDataFormatException(header, "not an C-array");

        ValueArray of = checker.check(header);
        Object ret = of.create(header);

        if (of instanceof OfBuffer buffer) {
            long pos = 0L;
            while (true) {
                var read = channel.read(buffer.buffer);
                if (read < 0) break;
                pos += read;
                if (!buffer.read(buffer.buffer, pos, null)) break;
            }
        } else {
            var descr = header.descr();
            boolean isLittle = descr.charAt(0) == '<';

            var valueSize = of.valueSize;
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
        }

        return new Read(header, ret);
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

    public static void write(Path file, boolean[] array) throws IOException {
        var of = new OfBoolean();
        of.checkFor(array);
        write(file, array, of);
    }

    public static <T> void write(Path file, T array, ValueArray<T> of) throws IOException {
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

    public static void write(OutputStream out, boolean[] array) throws IOException {
        var of = new OfBoolean();
        of.checkFor(array);
        write(out, array, of);
    }

    public static <T> void write(OutputStream out, T array, ValueArray<T> of) throws IOException {
        of.checkFor(array);
        write(Channels.newChannel(out), array, of);
    }

    private static <T> void write(WritableByteChannel channel, T array, ValueArray<T> of) throws IOException {
        writeHeader(channel, NumpyHeader.of(1, 0, of.descr(), false, of.shape()));

        if (of instanceof OfBuffer buffer) {
            var ret = (ByteBuffer) array;

            long pos = 0L;
            while (true) {
                if (!buffer.write(ret, pos, null)) break;

                pos += buffer.buffer.limit();
                channel.write(ret);
            }
        } else {
            var valueSize = of.valueSize;
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
        buffer.put((byte) header.majorVersion());
        buffer.put((byte) header.minorVersion());

        var data = header.data().getBytes();
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
