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

    /**
     * @param file .npy file
     * @return int[5][N] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
     * @throws IOException
     */
    public static int[][] read(Path file) throws IOException {
        try (var channel = Files.newByteChannel(file)) {
            return read(channel);
        }
    }

    /**
     * @param in
     * @return int[5][N] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
     * @throws IOException
     */
    public static int[][] read(InputStream in) throws IOException {
        return read(Channels.newChannel(in));
    }

    /**
     * @param channel
     * @return int[5][N] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
     * @throws IOException
     */
    private static int[][] read(ReadableByteChannel channel) throws IOException {
        var header = readHeader(channel);

        var shape = header.shape();
        if (shape.length != 2) throw new IOException("not an 2-d array : " + Arrays.toString(shape));
        // (N, (shank, col, row, state, category))
        var length = shape[0];
        var columns = shape[1];
        if (columns != 5) throw new IOException("not an (N, 5) array : " + Arrays.toString(shape));

        if (header.fortranOrder()) throw new IOException("not an (N, 5) C-array");

        var descr = header.descr();
        boolean isLittle = descr.charAt(0) == '<';
        var valueType = descr.charAt(1);
        var valueSize = Integer.parseInt(descr.substring(2));
        if (valueType != 'i') throw new IOException("not an (N, 5) int array, but : " + descr);
        if (valueSize != 1 && valueSize != 2 && valueSize != 4 && valueSize != 8)
            throw new IOException("not an (N, 5) int array, but : " + descr);

        var buffer = ByteBuffer.allocate(32 * 5);
        buffer.order(isLittle ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);

        var ret = new int[5][];
        for (int c = 0; c < 5; c++) ret[c] = new int[length];

        int r = 0;
        while (r < length) {
            buffer.clear();
            if (channel.read(buffer) < 0) throw new IOException("EOF");
            buffer.flip();

            while (buffer.remaining() >= 5) {
                for (int c = 0; c < 5; c++) {
                    switch (valueSize) {
                    case 1 -> ret[c][r] = buffer.get();
                    case 2 -> ret[c][r] = buffer.getShort();
                    case 4 -> ret[c][r] = buffer.getInt();
                    case 8 -> ret[c][r] = (int) buffer.getLong();
                    }
                }
                r++;
            }
        }

        return ret;
    }

    /**
     * @param file
     * @param array int[5][N] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
     * @throws IOException
     */
    public static void write(Path file, int[][] array) throws IOException {
        checkOutArray(array);
        try (var channel = Files.newByteChannel(file, CREATE, TRUNCATE_EXISTING, WRITE)) {
            write(channel, array);
        }
    }

    /**
     * @param out
     * @param array int[5][N] array that {0: shank, 1: column, 2: row, 3: state, 4: category}
     * @throws IOException
     */
    public static void write(OutputStream out, int[][] array) throws IOException {
        checkOutArray(array);
        write(Channels.newChannel(out), array);
    }

    private static void checkOutArray(int[][] array) {
        if (array.length != 5) throw new IllegalArgumentException("not an array[5][N]");
        var length = array[0].length;
        if (array[1].length != length) throw new IllegalArgumentException("not an array[5][N]");
        if (array[2].length != length) throw new IllegalArgumentException("not an array[5][N]");
        if (array[3].length != length) throw new IllegalArgumentException("not an array[5][N]");
        if (array[4].length != length) throw new IllegalArgumentException("not an array[5][N]");
    }

    private static void write(WritableByteChannel channel, int[][] array) throws IOException {
        var length = array[0].length;
        writeHeader(channel, NumpyHeader.of(1, 0, "<i4", false, new int[]{length, 5}));

        var buffer = ByteBuffer.allocate(32 * 5);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < length; i++) {
            for (int c = 0; c < 5; c++) {
                buffer.putInt(array[c][i]);
            }

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
