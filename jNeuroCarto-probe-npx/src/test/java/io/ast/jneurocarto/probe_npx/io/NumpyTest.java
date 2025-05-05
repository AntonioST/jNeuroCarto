package io.ast.jneurocarto.probe_npx.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


import static org.junit.jupiter.api.Assertions.*;

public class NumpyTest {

    record NumpyHeaderTestData(
      String data,
      String descr,
      boolean fortranOrder,
      int[] shape
    ) {
    }

    static List<NumpyHeaderTestData> numpyHeaderData() {
        return List.of(
          new NumpyHeaderTestData(
            "{'descr': '<i8', 'fortran_order': False, 'shape': (5120, 5), }                                                       \n",
            "<i8", false, new int[]{5120, 5}
          ),
          new NumpyHeaderTestData(
            "{'descr': '<f8', 'fortran_order': False, 'shape': (5120, 5), }                                                       \n",
            "<f8", false, new int[]{5120, 5}
          ),
          new NumpyHeaderTestData(
            "{'descr': '<f8', 'fortran_order': False, 'shape': (3, 2, 1), }                                                       \n",
            "<f8", false, new int[]{3, 2, 1}
          ),
          new NumpyHeaderTestData(
            "{'descr': '|u1', 'fortran_order': False, 'shape': (3, 2, 1), }                                                       \n",
            "|u1", false, new int[]{3, 2, 1}
          )
        );
    }

    @ParameterizedTest
    @MethodSource()
    void numpyHeaderData(NumpyHeaderTestData data) {
        var header = new Numpy.NumpyHeader(0, 0, data.data);
        assertEquals(data.data.strip(), header.data());
        assertEquals(data.descr, header.descr());
        assertEquals(data.fortranOrder, header.fortranOrder());
        assertArrayEquals(data.shape, header.shape());
    }

    @ParameterizedTest
    @MethodSource("numpyHeaderData")
    void numpyHeaderDataOf(NumpyHeaderTestData data) {
        assertEquals(data.data.strip(),
          Numpy.NumpyHeader.of(0, 0, data.descr, data.fortranOrder, data.shape).data());
    }

    @Test
    void readNumpyFile() throws IOException {
        var file = Path.of("src/test/resources/Fig3_example.blueprint.npy");
        assertTrue(Files.exists(file));
        var data = Numpy.read(file);
        assertEquals(5, data.length);
        assertEquals(5120, data[0].length);
        assertEquals(5120, data[1].length);
        assertEquals(5120, data[2].length);
        assertEquals(5120, data[3].length);
        assertEquals(5120, data[4].length);
    }

    @Test
    void readWriteNumpyHeader() throws IOException {
        var file = Path.of("src/test/resources/Fig3_example.blueprint.npy");
        assertTrue(Files.exists(file));

        try (var channel = Files.newByteChannel(file)) {
            byte[] data = new byte[128];
            channel.read(ByteBuffer.wrap(data));
            assertEquals((byte) '\n', data[127]);

            channel.position(0);
            var header = Numpy.readHeader(channel);

            var result = new ByteArrayOutputStream(128);
            Numpy.writeHeader(Channels.newChannel(result), header);

            assertArrayEquals(data, result.toByteArray());
        }
    }

    @Test
    void readWriteNumpyArray() throws IOException {
        var data = new int[5][];
        for (int i = 0; i < 5; i++) {
            data[i] = new int[12];
            for (int j = 0; j < 12; j++) {
                data[i][j] = (int) (64 * Math.random());
            }
        }
        assert2DArrayEquals(data, data);

        var file = Files.createTempFile(Path.of("target"), "test-", ".npy");

        try {
            Numpy.write(file, data);

            var back = Numpy.read(file);
            assert2DArrayEquals(data, back);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void assert2DArrayEquals(int[][] expect, int[][] actual) {
        assertEquals(expect.length, actual.length, () -> "expect[" + expect.length + "] != actual[" + actual.length + "]");
        for (int i = 0, length = expect.length; i < length; i++) {
            int j = i;
            assertArrayEquals(expect[i], actual[i], () -> "expect[" + j + "][*] != actual[" + j + "][*]");
        }
    }


}
