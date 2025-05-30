package io.ast.neurocarto.jmh;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChannelMapUtilTest {

    public record NpxTypeData(
      NpxProbeType type,
      int[] shanks,
      int[] electrodes
    ) {
        NpxTypeData(NpxProbeType type) {
            var shanks = new int[type.nShank()];
            for (int i = 0, length = shanks.length; i < length; i++) {
                shanks[i] = i;
            }
            var electrodes = new int[type.nElectrode()];
            for (int i = 0, length = electrodes.length; i < length; i++) {
                electrodes[i] = i;
            }
            this(type, shanks, electrodes);
        }

        public int nShank() {
            return shanks.length;
        }

        public int nElectrode() {
            return electrodes.length;
        }

        @Override
        public String toString() {
            return type().name();
        }
    }

    public static NpxTypeData[] DATA;

    @BeforeAll
    public static void initNpxTypeData() {
        DATA = new NpxTypeData[]{new NpxTypeData(NpxProbeType.NP0), new NpxTypeData(NpxProbeType.NP21), new NpxTypeData(NpxProbeType.NP24)};
    }

    private static void assert2DArrayEquals(int[][] expect, int[][] actual) {
        assertEquals(expect.length, actual.length, () -> "expect[" + expect.length + "] != actual[" + actual.length + "]");
        for (int i = 0, length = expect.length; i < length; i++) {
            int j = i;
            assertArrayEquals(expect[i], actual[i], () -> "expect[" + j + "][*] != actual[" + j + "][*]");
        }
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void electrodePosSCR(NpxTypeData data) {
        assert2DArrayEquals(
          ChannelMapUtil.electrodePosSCR(data.type),
          ChannelMapUtilPlain.electrodePosSCR(data.type)
        );
        assert2DArrayEquals(
          ChannelMapUtilPlain.electrodePosSCR(data.type),
          ChannelMapUtilVec.electrodePosSCR(data.type)
        );
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void electrodePosXY(NpxTypeData data) {
        assert2DArrayEquals(
          ChannelMapUtil.electrodePosXY(data.type),
          ChannelMapUtilPlain.electrodePosXY(data.type)
        );
        assert2DArrayEquals(
          ChannelMapUtilPlain.electrodePosXY(data.type),
          ChannelMapUtilVec.electrodePosXY(data.type)
        );
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void e2xy(NpxTypeData data) {
        for (int i = 0, length = data.shanks.length; i < length; i++) {
            var shank = data.shanks[i];
            assert2DArrayEquals(
              ChannelMapUtil.e2xy(data.type, shank, data.electrodes),
              ChannelMapUtilPlain.e2xy(data.type, shank, data.electrodes)
            );
        }
        for (int i = 0, length = data.shanks.length; i < length; i++) {
            var shank = data.shanks[i];
            assert2DArrayEquals(
              ChannelMapUtilPlain.e2xy(data.type, shank, data.electrodes),
              ChannelMapUtilVec.e2xy(data.type, shank, data.electrodes)
            );
        }
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void e2cr(NpxTypeData data) {
        assert2DArrayEquals(
          ChannelMapUtil.e2cr(data.type, data.electrodes),
          ChannelMapUtilPlain.e2cr(data.type, data.electrodes)
        );
        assert2DArrayEquals(
          ChannelMapUtilPlain.e2cr(data.type, data.electrodes),
          ChannelMapUtilVec.e2cr(data.type, data.electrodes)
        );
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void e2xyFromScr(NpxTypeData data) {
        var scr = ChannelMapUtilPlain.e2cr(data.type, data.electrodes);
        assert2DArrayEquals(
          ChannelMapUtil.e2xy(data.type, scr),
          ChannelMapUtilPlain.e2xy(data.type, scr)
        );
        assert2DArrayEquals(
          ChannelMapUtilPlain.e2xy(data.type, scr),
          ChannelMapUtilVec.e2xy(data.type, scr)
        );
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void cr2e(NpxTypeData data) {
        var scr = ChannelMapUtilPlain.e2cr(data.type, data.electrodes);
        assertArrayEquals(
          ChannelMapUtil.cr2e(data.type, scr),
          ChannelMapUtilPlain.cr2e(data.type, scr)
        );
        assertArrayEquals(
          ChannelMapUtilPlain.cr2e(data.type, scr),
          ChannelMapUtilVec.cr2e(data.type, scr)
        );
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void e2c(NpxTypeData data) {
        var scr = ChannelMapUtilPlain.e2cr(data.type, data.electrodes);
        assertArrayEquals(
          ChannelMapUtil.e2c(data.type, scr),
          ChannelMapUtilPlain.e2c(data.type, scr)
        );
        assertArrayEquals(
          ChannelMapUtilPlain.e2c(data.type, scr),
          ChannelMapUtilVec.e2c(data.type, scr)
        );
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void e2cb(NpxTypeData data) {
        for (int i = 0, length = data.shanks.length; i < length; i++) {
            var shank = data.shanks[i];
            assert2DArrayEquals(
              ChannelMapUtil.e2cb(data.type, shank, data.electrodes),
              ChannelMapUtilPlain.e2cb(data.type, shank, data.electrodes)
            );
        }
        for (int i = 0, length = data.shanks.length; i < length; i++) {
            var shank = data.shanks[i];
            assert2DArrayEquals(
              ChannelMapUtilPlain.e2cb(data.type, shank, data.electrodes),
              ChannelMapUtilVec.e2cb(data.type, shank, data.electrodes)
            );
        }
    }

    @ParameterizedTest(name = "{0}")
    @FieldSource("DATA")
    public void e2cbWithShank(NpxTypeData data) {
        var s = data.nShank();
        int e = data.nElectrode();
        int[] shank = new int[e];
        if (s == 1) {
            Arrays.fill(shank, 0);
        } else {
            for (int i = 0; i < e; i++) {
                shank[i] = (int) (Math.random() * s);
            }
        }

        assert2DArrayEquals(
          ChannelMapUtil.e2cb(data.type, shank, data.electrodes),
          ChannelMapUtilPlain.e2cb(data.type, shank, data.electrodes)
        );
        assert2DArrayEquals(
          ChannelMapUtilPlain.e2cb(data.type, shank, data.electrodes),
          ChannelMapUtilVec.e2cb(data.type, shank, data.electrodes)
        );
    }
}
