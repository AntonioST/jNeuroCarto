package io.ast.neurocarto.jmh;

import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

public final class ChannelMapUtilValue {

    private ChannelMapUtilValue() {
        throw new RuntimeException();
    }

    public value record

    XY(int s, int x, int y) {
    }

    public value record

    CR(int s, int c, int r) {
    }

    public value record

    CB(int channel, int bank) {
    }


    /**
     * @see ChannelMapUtil#electrodePosSCR(NpxProbeType)
     */
    public static CR[] electrodePosSCR(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();

        var ret = new CR[ns * ne];

        for (int i = 0; i < ns * ne; i++) {
            ret[i] = new CR(i / ne, (i % ne) % nc, (i % ne) / nc);
        }

        return ret;
    }

    /**
     * @see ChannelMapUtil#electrodePosXY(NpxProbeType)
     */
    public static XY[] electrodePosXY(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();
        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        var ret = new XY[ns * ne];

        for (int i = 0; i < ns * ne; i++) {
            var s = i / ne;
            var v = i % ne;
            var r = v / nc;
            var c = v % nc;

            ret[i] = new XY(s, (int) (s * ps + c * pc), (int) (pr * r));
        }

        return ret;
    }

    /**
     * @see ChannelMapUtil#e2xy(NpxProbeType, int, int[])
     */
    public static XY[] e2xy(NpxProbeType type, int shank, int[] electrode) {
        var cr = e2cr(type, electrode);
        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        var ret = new XY[electrode.length];

        for (int i = 0, length = electrode.length; i < length; i++) {
            var t = cr[i];
            ret[i] = new XY(shank, (int) (t.c * pc + shank * ps), (int) (t.r * pr));
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2cr(NpxProbeType, int[])
     */
    public static CR[] e2cr(NpxProbeType type, int[] electrode) {
        var nc = type.nColumnPerShank();

        var ret = new CR[electrode.length];

        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[i] = new CR(0, electrode[i] % nc, electrode[i] / nc);
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2xy(NpxProbeType, int[][])
     */
    public static XY[] e2xy(NpxProbeType type, CR[] scr) {
        var ret = new XY[scr.length];

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        for (int i = 0, length = scr.length; i < length; i++) {
            var t = scr[i];
            ret[i] = new XY(t.s, (int) (t.c * pc + t.s * ps), (int) (t.r * pr));
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#cr2e(NpxProbeType, int[][])
     */
    public static int[] cr2e(NpxProbeType type, CR[] scr) {
        var ret = new int[scr.length];

        var nc = type.nColumnPerShank();

        for (int i = 0, length = ret.length; i < length; i++) {
            var t = scr[i];
            ret[i] = t.c + t.r * nc;
        }

        return ret;
    }

    /*=======================*
     * E-to-C/C-to-E mapping *
     *=======================*/

    /**
     * @see ChannelMapUtil#e2c(NpxProbeType, int[][])
     */
    public static CB[] e2c(NpxProbeType type, CR[] scr) {
        var e = cr2e(type, scr);
        var shank = new int[scr.length];
        for (int i = 0, length = shank.length; i < length; i++) {
            shank[i] = scr[i].s;
        }
        return e2cb(type, shank, e);
    }

    public static int[] c2e(NpxProbeType type, CB[] channel, int shank) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> c2e(channel, ChannelMapUtilValue::c2e21);
            case NpxProbeType.NP24Base _ -> c2e(channel, shank, ChannelMapUtilValue::c2e24);
            case NpxProbeType.NP1110 _ -> c2e(channel, ChannelMapUtilValue::c2e1110);
            case NpxProbeType.NP2020 _ -> c2e(channel, shank, ChannelMapUtilValue::c2e2020);
            case NpxProbeType.NP3010 _ -> c2e(channel, ChannelMapUtilValue::c2e3010);
            case NpxProbeType.NP3020 _ -> c2e(channel, shank, ChannelMapUtilValue::c2e3020);
            default -> c2e(channel, ChannelMapUtilValue::c2e0);
        };
    }

    public static int[] c2e(NpxProbeType type, CB[] channel, int[] shank) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> c2e(channel, ChannelMapUtilValue::c2e21);
            case NpxProbeType.NP24Base _ -> c2e(channel, shank, ChannelMapUtilValue::c2e24);
            case NpxProbeType.NP1110 _ -> c2e(channel, ChannelMapUtilValue::c2e1110);
            case NpxProbeType.NP2020 _ -> c2e(channel, shank, ChannelMapUtilValue::c2e2020);
            case NpxProbeType.NP3010 _ -> c2e(channel, ChannelMapUtilValue::c2e3010);
            case NpxProbeType.NP3020 _ -> c2e(channel, shank, ChannelMapUtilValue::c2e3020);
            default -> c2e(channel, ChannelMapUtilValue::c2e0);
        };
    }


    private static int[] c2e(CB[] channel, IntBinaryOperator func) {
        int length = channel.length;
        var ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = func.applyAsInt(channel[i].channel, channel[i].bank);
        }
        return ret;
    }

    @FunctionalInterface
    private interface IntTriFunction {
        int apply(int a, int b, int c);
    }

    private static int[] c2e(CB[] channel, int shank, IntTriFunction func) {
        int length = channel.length;
        var ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = func.apply(channel[i].channel, channel[i].bank, shank);
        }
        return ret;
    }

    private static int[] c2e(CB[] channel, int[] shank, IntTriFunction func) {
        int length = channel.length;
        if (shank.length != length) throw new IllegalArgumentException();
        var ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = func.apply(channel[i].channel, channel[i].bank, shank[i]);
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int, int[])
     */
    public static CB[] e2cb(NpxProbeType type, int shank, int[] electrode) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> e2cb(electrode, ChannelMapUtilValue::e2c21);
            case NpxProbeType.NP24Base _ -> e2cb(shank, electrode, ChannelMapUtilValue::e2c24);
            case NpxProbeType.NP1110 _ -> e2cb(electrode, ChannelMapUtilValue::e2c1110);
            case NpxProbeType.NP2020 _ -> e2cb(shank, electrode, ChannelMapUtilValue::e2c2020);
            case NpxProbeType.NP3010 _ -> e2cb(electrode, ChannelMapUtilValue::e2c3010);
            case NpxProbeType.NP3020 _ -> e2cb(shank, electrode, ChannelMapUtilValue::e2c3020);
            default -> e2cb(electrode, ChannelMapUtilValue::e2c0);
        };
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int[], int[])
     */
    public static CB[] e2cb(NpxProbeType type, int[] shank, int[] electrode) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> e2cb(electrode, ChannelMapUtilValue::e2c21);
            case NpxProbeType.NP24Base _ -> e2cb(shank, electrode, ChannelMapUtilValue::e2c24);
            case NpxProbeType.NP1110 _ -> e2cb(electrode, ChannelMapUtilValue::e2c1110);
            case NpxProbeType.NP2020 _ -> e2cb(shank, electrode, ChannelMapUtilValue::e2c2020);
            case NpxProbeType.NP3010 _ -> e2cb(electrode, ChannelMapUtilValue::e2c3010);
            case NpxProbeType.NP3020 _ -> e2cb(shank, electrode, ChannelMapUtilValue::e2c3020);
            default -> e2cb(electrode, ChannelMapUtilValue::e2c0);
        };
    }

    private static CB[] e2cb(int[] electrode, IntFunction<CB> func) {
        var ret = new CB[electrode.length];
        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[i] = func.apply(electrode[i]);
        }
        return ret;
    }

    @FunctionalInterface
    private interface BiIntFunction<R> {
        R apply(int a, int b);
    }


    private static CB[] e2cb(int shank, int[] electrode, BiIntFunction<CB> func) {
        var ret = new CB[electrode.length];
        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[i] = func.apply(shank, electrode[i]);
        }
        return ret;
    }

    private static CB[] e2cb(int[] shank, int[] electrode, BiIntFunction<CB> func) {
        if (shank.length != electrode.length) throw new IndexOutOfBoundsException();

        var ret = new CB[electrode.length];
        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[i] = func.apply(shank[i], electrode[i]);
        }
        return ret;
    }

    /*===========================================*
     * E-to-C/C-to-E mapping for each probe type *
     *===========================================*/

    /**
     * @see ChannelMapUtil#c2e0(int, int)
     */
    public static int c2e0(int channel, int bank) {
        var n = NpxProbeType.np0.nChannel();
        return bank * n + channel % n;
    }

    /**
     * @see ChannelMapUtil#e2c0(int)
     */
    public static CB e2c0(int electrode) {
        var n = NpxProbeType.np0.nChannel();
        return new CB(electrode % n, electrode / n);
    }

    static final int[][] ELECTRODE_MAP_21 = new int[][]{
      {1, 7, 5, 3},
      {0, 4, 8, 12},
    };

    private static int np21IndexOfRow(int bank, int row, int col) {
        var bf = ELECTRODE_MAP_21[0][bank];
        var ba = ELECTRODE_MAP_21[1][bank];
        for (int r = 0; r < 16; r++) {
            if ((r * bf + col * ba) % 16 == row) {
                return r;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * @see ChannelMapUtil#c2e21(int, int)
     */
    public static int c2e21(int channel, int bank) {
        var block = channel / 32;
        var index = channel % 32;
        var row = index / 2;
        var col = index % 2;
        row = np21IndexOfRow(bank, row, col);
        return bank * 384 + block * 32 + row * 2 + col;
    }

    /**
     * @see ChannelMapUtil#e2c21(int)
     */
    public static CB e2c21(int electrode) {
        var n = NpxProbeType.np21.nChannel();
        var bf = ELECTRODE_MAP_21[0];
        var ba = ELECTRODE_MAP_21[1];
        var bank = electrode / n;
        var e1 = electrode % n;
        var block = e1 / 32/*type.nElectrodePerBlock()*/;
        var e2 = e1 % 32;
        var row = e2 / 2;
        var column = e2 % 2;
        var channel = 2 * ((row * bf[bank] + column * ba[bank]) % 16) + 32 * block + column;
        return new CB(channel, bank);
    }

    static final int[][] ELECTRODE_MAP_24 = new int[][]{
      {0, 2, 4, 6, 5, 7, 1, 3},
      {1, 3, 5, 7, 4, 6, 0, 2},
      {4, 6, 0, 2, 1, 3, 5, 7},
      {5, 7, 1, 3, 0, 2, 4, 6},
    };

    private static int np24IndexOfBlock(int shank, int block) {
        var s = ELECTRODE_MAP_24[shank];
        for (int i = 0; i < 8; i++) {
            if (s[i] == block) return i;
        }
        throw new IllegalArgumentException();
    }

    /**
     * @see ChannelMapUtil#c2e24(int, int, int)
     */
    public static int c2e24(int channel, int bank, int shank) {
        var block = channel / 48;
        var index = channel % 48;
        block = np24IndexOfBlock(shank, block);
        return bank * 384 + block * 48 + index;
    }

    /**
     * @see ChannelMapUtil#e2c24(int, int)
     */
    public static CB e2c24(int shank, int electrode) {
        var n = NpxProbeType.np24.nChannel();
        var bank = electrode / n;
        var e1 = electrode % n;
        var b1 = e1 / 48/*type.nElectrodePerBlock()*/;
        var index = e1 % 48;
        var block = ELECTRODE_MAP_24[shank][b1];
        return new CB(48 * block + index, bank);
    }

    /**
     * @see ChannelMapUtil#np1110Group(int)
     */
    public static int np1110Group(int channel) {
        var c = channel % 384;
        return 2 * (c / 32) + (c % 2);
    }

    /**
     * @see ChannelMapUtil#np1110Row(int, int, int)
     */
    public static int np1110Row(int channel, int bank, int group) {
        var groupRow = group / 4;
        var inGroupRow = ((channel % 64) % 32) / 4;
        var bankRow = 8 * groupRow + (channel % 2 == 0 ? inGroupRow : 7 - inGroupRow);
        return 48 * bank + bankRow;
    }

    static final int[][] ELECTRODE_MAP_1110 = new int[][]{
      {0, 3, 1, 2},
      {1, 2, 0, 3},
    };

    /**
     * @see ChannelMapUtil#np1110Col(int, int, int)
     */
    public static int np1110Col(int channel, int bank, int group) {
        var groupCol = ELECTRODE_MAP_1110[bank % 2][group % 4];
        var crossed = (bank / 4) % 2;
        var inGroupCol = (((channel % 64) % 32) / 2) % 2;
        inGroupCol = inGroupCol ^ crossed;
        return 2 * groupCol + (channel % 2 == 0 ? inGroupCol : 1 - inGroupCol);
    }

    /**
     * @see ChannelMapUtil#c2e1110(int, int)
     */
    public static int c2e1110(int channel, int bank) {
        var g = np1110Group(channel);
        var r = np1110Row(channel, bank, g);
        var c = np1110Col(channel, bank, g);
        return r * 8 + c;
    }

    private static final LazyConstant<int[]> ELECTRODE_MAP_1110_CACHE = LazyConstant.of(ChannelMapUtilValue::initNp1110E2CCache);

    private static int[] initNp1110E2CCache() {
        var type = NpxProbeType.np1110;
        var channels = type.nChannel();
        assert type.nElectrodePerShank() / channels == 16;
        var banks = 16;

        var ret = new int[banks * channels];
        int e = 0;
        for (int b = 0; b < banks; b++) {
            for (int c = 0; c < channels; c++) {
                ret[e++] = c2e1110(c, b);
            }
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2c1110(int)
     */
    public static CB e2c1110(int electrode) {
        var cache = ELECTRODE_MAP_1110_CACHE.get();
        for (int i = 0, length = cache.length; i < length; i++) {
            if (cache[i] == electrode) {
                return new CB(i % 384, i / 384);
            }
        }
        throw new IllegalArgumentException();
    }

    public static int c2e2020(int channel, int bank, int shank) {
        return bank * 384 + channel % 384;
    }

    public static CB e2c2020(int shank, int electrode) {
        var b = electrode / 384;
        var c = electrode % 384;
        return new CB(shank * 384 + c, b);
    }

    public static int c2e3010(int channel, int bank) {
        return bank * 912 + channel;
    }

    public static CB e2c3010(int electrode) {
        var b = electrode / 912;
        var c = electrode % 912;
        return new CB(c, b);
    }

    static final int[][] ELECTRODE_MAP_3020 = new int[][]{//  4 shanks X 32 blocks, 99 = forbidden
      {0, 16, 1, 17, 2, 18, 3, 99, 4, 99, 5, 99, 6, 99, 7, 99, 8, 99, 9, 99, 10, 99, 11, 99, 12, 99, 13, 99, 14, 99, 15, 99},
      {16, 0, 17, 1, 18, 2, 99, 3, 99, 4, 99, 5, 99, 6, 99, 7, 99, 8, 99, 9, 99, 10, 99, 11, 99, 12, 99, 13, 99, 14, 99, 15},
      {8, 99, 9, 99, 10, 99, 11, 99, 12, 99, 13, 99, 14, 99, 15, 99, 0, 16, 1, 17, 2, 18, 3, 99, 4, 99, 5, 99, 6, 99, 7, 99},
      {99, 8, 99, 9, 99, 10, 99, 11, 99, 12, 99, 13, 99, 14, 99, 15, 16, 0, 17, 1, 18, 2, 99, 3, 99, 4, 99, 5, 99, 6, 99, 7},
    };

    private static int np3020IndexOfBlock(int shank, int block) {
        var s = ELECTRODE_MAP_3020[shank];
        for (int i = 0, length = s.length; i < length; i++) {
            if (s[i] == block) return i;
        }
        return -1;
    }

    public static int c2e3020(int channel, int bank, int shank) {
        var b = channel / 48;
        var i = channel % 48;
        b = ELECTRODE_MAP_3020[shank][b];
        if (b == 99) throw new IllegalArgumentException("Illegal channel");
        return bank * 912 + b * 48 + i;
    }

    public static CB e2c3020(int shank, int electrode) {
        var b = electrode / 912;
        var e = electrode % 912;
        var k = e / 48; // block
        var i = e % 48; // index
        k = np3020IndexOfBlock(shank, k);
        return new CB(k * 48 + i, b);
    }

}
