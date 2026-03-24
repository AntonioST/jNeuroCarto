package io.ast.neurocarto.jmh;

import java.util.Arrays;
import java.util.function.IntBinaryOperator;
import java.util.function.IntFunction;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

public final class ChannelMapUtilPlain {

    private ChannelMapUtilPlain() {
        throw new RuntimeException();
    }

    /**
     * @see ChannelMapUtil#empty(int)
     */
    static int[][] empty(int length) {
        var ret = new int[3][];
        ret[0] = new int[length];
        ret[1] = new int[length];
        ret[2] = new int[length];
        return ret;
    }

    /**
     * @see ChannelMapUtil#empty(int, int)
     */
    static int[][] empty(int n, int length) {
        var ret = new int[n][];
        for (int i = 0; i < n; i++) {
            ret[i] = new int[length];
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#check2N(int[][])
     */
    static int check2N(int[][] a) {
        if (a.length != 2) throw new IllegalArgumentException("Not a 2,N array");
        var length = a[0].length;
        if (a[1].length != length) throw new IllegalArgumentException("Not a 2,N array: inconsistent length on 2nd row");
        return length;
    }

    /**
     * @see ChannelMapUtil#check3N(int[][])
     */
    static int check3N(int[][] a) {
        if (a.length != 3) throw new IllegalArgumentException("Not a 3,N array");
        var length = a[0].length;
        if (a[1].length != length) throw new IllegalArgumentException("Not a 3,N array: inconsistent length on 2nd row");
        if (a[2].length != length) throw new IllegalArgumentException("Not a 3,N array: inconsistent length on 3rd row");
        return length;
    }

    /**
     * @see ChannelMapUtil#like(int[][], boolean)
     */
    static int[][] like(int[][] a, boolean cloneFirst) {
        var length = check3N(a);

        var ret = new int[3][];
        ret[0] = cloneFirst ? a[0].clone() : new int[length];
        ret[1] = new int[length];
        ret[2] = new int[length];

        return ret;
    }

    /**
     * @see ChannelMapUtil#electrodePosSCR(NpxProbeType)
     */
    public static int[][] electrodePosSCR(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();

        var ret = empty(ns * ne);

        for (int i = 0; i < ns * ne; i++) {
            // shank
            ret[0][i] = (i / ne);
            // column
            ret[1][i] = (i % ne) % nc;
            // row
            ret[2][i] = (i % ne) / nc;
        }

        return ret;
    }

    /**
     * @see ChannelMapUtil#electrodePosXY(NpxProbeType)
     */
    public static int[][] electrodePosXY(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();
        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        var ret = empty(ns * ne);

        for (int i = 0; i < ns * ne; i++) {
            var s = i / ne;
            var v = i % ne;
            var r = v / nc;
            var c = v % nc;

            // s
            ret[0][i] = s;
            // x
            ret[1][i] = (int) (s * ps + c * pc);
            // y
            ret[2][i] = (int) (pr * r);
        }

        return ret;
    }

    /**
     * @see ChannelMapUtil#e2xy(NpxProbeType, int, int[])
     */
    public static int[][] e2xy(NpxProbeType type, int shank, int[] electrode) {
        var cr = e2cr(type, electrode);
        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        var ret = empty(electrode.length);
        Arrays.fill(ret[0], shank);

        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[1][i] = (int) (cr[1][i] * pc + shank * ps);
            ret[2][i] = (int) (cr[2][i] * pr);
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2cr(NpxProbeType, int[])
     */
    public static int[][] e2cr(NpxProbeType type, int[] electrode) {
        var nc = type.nColumnPerShank();

        var ret = empty(electrode.length);

        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[1][i] = electrode[i] % nc;
            ret[2][i] = electrode[i] / nc;
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2xy(NpxProbeType, int[][])
     */
    public static int[][] e2xy(NpxProbeType type, int[][] scr) {
        var ret = like(scr, true);

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        for (int i = 0, length = ret[0].length; i < length; i++) {
            ret[1][i] = (int) (scr[1][i] * pc + scr[0][i] * ps);
            ret[2][i] = (int) (scr[2][i] * pr);
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#cr2e(NpxProbeType, int[][])
     */
    public static int[] cr2e(NpxProbeType type, int[][] scr) {
        var ret = new int[check3N(scr)];

        var nc = type.nColumnPerShank();

        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = scr[1][i] + scr[2][i] * nc;
        }

        return ret;
    }

    /*=======================*
     * E-to-C/C-to-E mapping *
     *=======================*/

    /**
     * @see ChannelMapUtil#e2c(NpxProbeType, int[][])
     */
    public static int[] e2c(NpxProbeType type, int[][] scr) {
        var e = cr2e(type, scr);
        var cb = e2cb(type, scr[0], e);
        return cb[0];
    }

    public static int[] c2e(NpxProbeType type, int[][] channel, int shank) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> c2e(channel, ChannelMapUtilPlain::c2e21);
            case NpxProbeType.NP24Base _ -> c2e(channel, shank, ChannelMapUtilPlain::c2e24);
            case NpxProbeType.NP1110 _ -> c2e(channel, ChannelMapUtilPlain::c2e1110);
            case NpxProbeType.NP2020 _ -> c2e(channel, shank, ChannelMapUtilPlain::c2e2020);
            case NpxProbeType.NP3010 _ -> c2e(channel, ChannelMapUtilPlain::c2e3010);
            case NpxProbeType.NP3020 _ -> c2e(channel, shank, ChannelMapUtilPlain::c2e3020);
            default -> c2e(channel, ChannelMapUtilPlain::c2e0);
        };
    }

    public static int[] c2e(NpxProbeType type, int[][] channel, int[] shank) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> c2e(channel, ChannelMapUtilPlain::c2e21);
            case NpxProbeType.NP24Base _ -> c2e(channel, shank, ChannelMapUtilPlain::c2e24);
            case NpxProbeType.NP1110 _ -> c2e(channel, ChannelMapUtilPlain::c2e1110);
            case NpxProbeType.NP2020 _ -> c2e(channel, shank, ChannelMapUtilPlain::c2e2020);
            case NpxProbeType.NP3010 _ -> c2e(channel, ChannelMapUtilPlain::c2e3010);
            case NpxProbeType.NP3020 _ -> c2e(channel, shank, ChannelMapUtilPlain::c2e3020);
            default -> c2e(channel, ChannelMapUtilPlain::c2e0);
        };
    }

    private static int[] c2e(int[][] channel, IntBinaryOperator func) {
        int length = check2N(channel);
        var ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = func.applyAsInt(channel[0][i], channel[1][i]);
        }
        return ret;
    }

    @FunctionalInterface
    private interface IntTriFunction {
        int apply(int a, int b, int c);
    }

    private static int[] c2e(int[][] channel, int shank, IntTriFunction func) {
        int length = check2N(channel);
        var ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = func.apply(channel[0][i], channel[1][i], shank);
        }
        return ret;
    }

    private static int[] c2e(int[][] channel, int[] shank, IntTriFunction func) {
        int length = check2N(channel);
        if (shank.length != length) throw new IllegalArgumentException();
        var ret = new int[length];
        for (int i = 0; i < length; i++) {
            ret[i] = func.apply(channel[0][i], channel[1][i], shank[i]);
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int, int[])
     */
    public static int[][] e2cb(NpxProbeType type, int shank, int[] electrode) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> e2cb(electrode, ChannelMapUtilPlain::e2c21);
            case NpxProbeType.NP24Base _ -> e2cb(shank, electrode, ChannelMapUtilPlain::e2c24);
            case NpxProbeType.NP1110 _ -> e2cb(electrode, ChannelMapUtilPlain::e2c1110);
            case NpxProbeType.NP2020 _ -> e2cb(shank, electrode, ChannelMapUtilPlain::e2c2020);
            case NpxProbeType.NP3010 _ -> e2cb(electrode, ChannelMapUtilPlain::e2c3010);
            case NpxProbeType.NP3020 _ -> e2cb(shank, electrode, ChannelMapUtilPlain::e2c3020);
            default -> e2cb(electrode, ChannelMapUtilPlain::e2c0);
        };
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int[], int[])
     */
    public static int[][] e2cb(NpxProbeType type, int[] shank, int[] electrode) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> e2cb(electrode, ChannelMapUtilPlain::e2c21);
            case NpxProbeType.NP24Base _ -> e2cb(shank, electrode, ChannelMapUtilPlain::e2c24);
            case NpxProbeType.NP1110 _ -> e2cb(electrode, ChannelMapUtilPlain::e2c1110);
            case NpxProbeType.NP2020 _ -> e2cb(shank, electrode, ChannelMapUtilPlain::e2c2020);
            case NpxProbeType.NP3010 _ -> e2cb(electrode, ChannelMapUtilPlain::e2c3010);
            case NpxProbeType.NP3020 _ -> e2cb(shank, electrode, ChannelMapUtilPlain::e2c3020);
            default -> e2cb(electrode, ChannelMapUtilPlain::e2c0);
        };
    }

    private static int[][] e2cb(int[] electrode, IntFunction<ChannelMapUtil.CB> func) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = func.apply(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    @FunctionalInterface
    private interface BiIntFunction<R> {
        R apply(int a, int b);
    }


    private static int[][] e2cb(int shank, int[] electrode, BiIntFunction<ChannelMapUtil.CB> func) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = func.apply(shank, electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    private static int[][] e2cb(int[] shank, int[] electrode, BiIntFunction<ChannelMapUtil.CB> func) {
        if (shank.length != electrode.length) throw new IndexOutOfBoundsException();

        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = func.apply(shank[i], electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
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
    public static ChannelMapUtil.CB e2c0(int electrode) {
        var n = NpxProbeType.np0.nChannel();
        return new ChannelMapUtil.CB(electrode % n, electrode / n);
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
    public static ChannelMapUtil.CB e2c21(int electrode) {
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
        return new ChannelMapUtil.CB(channel, bank);
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
    public static ChannelMapUtil.CB e2c24(int shank, int electrode) {
        var n = NpxProbeType.np24.nChannel();
        var bank = electrode / n;
        var e1 = electrode % n;
        var b1 = e1 / 48/*type.nElectrodePerBlock()*/;
        var index = e1 % 48;
        var block = ELECTRODE_MAP_24[shank][b1];
        return new ChannelMapUtil.CB(48 * block + index, bank);
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

    private static final LazyConstant<int[]> ELECTRODE_MAP_1110_CACHE = LazyConstant.of(ChannelMapUtilPlain::initNp1110E2CCache);

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
    public static ChannelMapUtil.CB e2c1110(int electrode) {
        var cache = ELECTRODE_MAP_1110_CACHE.get();
        for (int i = 0, length = cache.length; i < length; i++) {
            if (cache[i] == electrode) {
                var b = i / 384;
                var c = i % 384;
                return new ChannelMapUtil.CB(c, b);
            }
        }
        throw new IllegalArgumentException();
    }

    public static int c2e2020(int channel, int bank, int shank) {
        return bank * 384 + channel % 384;
    }

    public static ChannelMapUtil.CB e2c2020(int shank, int electrode) {
        var b = electrode / 384;
        var c = electrode % 384;
        return new ChannelMapUtil.CB(shank * 384 + c, b);
    }

    public static int c2e3010(int channel, int bank) {
        return bank * 912 + channel;
    }

    public static ChannelMapUtil.CB e2c3010(int electrode) {
        var b = electrode / 912;
        var c = electrode % 912;
        return new ChannelMapUtil.CB(c, b);
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

    public static ChannelMapUtil.CB e2c3020(int shank, int electrode) {
        var b = electrode / 912;
        var e = electrode % 912;
        var k = e / 48; // block
        var i = e % 48; // index
        k = np3020IndexOfBlock(shank, k);
        return new ChannelMapUtil.CB(k * 48 + i, b);
    }

}
