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

import org.jspecify.annotations.NullMarked;

import static java.nio.file.StandardOpenOption.*;

/// * [Numpy format](https://numpy.org/devdocs/reference/generated/numpy.lib.format.html)
/// * [NEP-001](https://numpy.org/neps/nep-0001-npy-format.html)
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

    public static ValueArray<boolean[]> ofBoolean() {
        return new OfBoolean();
    }

    public static ValueArray<boolean[][]> ofD2Boolean() {
        return new OfD2Boolean();
    }

    public static ValueArray<boolean[][][]> ofD3Boolean() {
        return new OfD3Boolean();
    }

    public static ValueArray<int[]> ofInt() {
        return new OfInt();
    }

    public static ValueArray<int[][]> ofD2Int() {
        return new OfD2Int();
    }

    public static ValueArray<int[][]> ofD2Int(boolean columnFirst) {
        return new OfD2Int(columnFirst);
    }

    public static ValueArray<int[][][]> ofD3Int() {
        return new OfD3Int();
    }

    public static ValueArray<FlatIntArray> ofFlattenInt() {
        return new OfFlattenInt();
    }

    public static ValueArray<double[]> ofDouble() {
        return new OfDouble();
    }

    public static ValueArray<double[][]> ofD2Double() {
        return new OfD2Double();
    }

    public static ValueArray<double[][]> ofD2Double(boolean columnFirst) {
        return new OfD2Double(columnFirst);
    }

    public static ValueArray<double[][][]> ofD3Double() {
        return new OfD3Double();
    }


    public static ValueArray<FlatDoubleArray> ofFlattenDouble() {
        return new OfFlattenDouble();
    }

    public static ValueArray<NumpyHeader> ofHeader() {
        return new OfHeader();
    }

    /*======*
     * Read *
     *======*/

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
        } else if (of instanceof OfHeader _) {
            // skip
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

    /*========================*
     * Read with unknown type *
     *========================*/

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
        } else if (of instanceof OfHeader _) {
            // skip
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

    /*=======*
     * Write *
     *=======*/

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

    public static void write(Path file, boolean[][] array) throws IOException {
        var of = new OfD2Boolean();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, boolean[][][] array) throws IOException {
        var of = new OfD3Boolean();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, FlatIntArray array) throws IOException {
        var of = new OfFlattenInt();
        of.checkFor(array);
        write(file, array, of);
    }

    public static void write(Path file, FlatDoubleArray array) throws IOException {
        var of = new OfFlattenDouble();
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

    public static void write(OutputStream out, boolean[][] array) throws IOException {
        var of = new OfD2Boolean();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, boolean[][][] array) throws IOException {
        var of = new OfD3Boolean();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, FlatIntArray array) throws IOException {
        var of = new OfFlattenInt();
        of.checkFor(array);
        write(out, array, of);
    }

    public static void write(OutputStream out, FlatDoubleArray array) throws IOException {
        var of = new OfFlattenDouble();
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
        } else if (of instanceof OfHeader _) {
            throw new UnsupportedOperationException("write header");
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

    /*===================*
     * header read/write *
     *===================*/

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
