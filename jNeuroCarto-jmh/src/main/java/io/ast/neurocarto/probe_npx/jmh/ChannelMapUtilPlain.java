package io.ast.neurocarto.probe_npx.jmh;

import java.util.Arrays;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.NpxProbeType;

public final class ChannelMapUtilPlain {

    static final int[][] ELECTRODE_MAP_21 = new int[][]{
      {1, 7, 5, 3},
      {0, 4, 8, 12},
    };

    static final int[][] ELECTRODE_MAP_24 = new int[][]{
      {0, 2, 4, 6, 5, 7, 1, 3},
      {1, 3, 5, 7, 4, 6, 0, 2},
      {4, 6, 0, 2, 1, 3, 5, 7},
      {5, 7, 1, 3, 0, 2, 4, 6},
    };

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
     * @see ChannelMapUtil#check(int[][])
     */
    static int check(int[][] a) {
        if (a.length != 3) throw new IllegalArgumentException();
        var length = a[0].length;
        if (a[1].length != length) throw new IllegalArgumentException();
        if (a[2].length != length) throw new IllegalArgumentException();
        return length;
    }

    /**
     * @see ChannelMapUtil#like(int[][], boolean)
     */
    static int[][] like(int[][] a, boolean cloneFirst) {
        var length = check(a);

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
            ret[1][i] = s * ps + c * pc;
            // y
            ret[2][i] = pr * r;
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
            ret[1][i] = cr[1][i] * pc + shank * ps;
            ret[2][i] = cr[2][i] * pr;
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
            ret[1][i] = scr[1][i] * pc + scr[0][i] * ps;
            ret[2][i] = scr[2][i] * pr;
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#cr2e(NpxProbeType, int[][])
     */
    public static int[] cr2e(NpxProbeType type, int[][] scr) {
        var ret = new int[check(scr)];

        var nc = type.nColumnPerShank();

        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = scr[1][i] + scr[2][i] * nc;
        }

        return ret;
    }

    /**
     * @see ChannelMapUtil#e2c(NpxProbeType, int[][])
     */
    public static int[] e2c(NpxProbeType type, int[][] scr) {
        var e = cr2e(type, scr);
        var cb = e2cb(type, scr[0], e);
        return cb[0];
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int, int)
     */
    public static ChannelMapUtil.CB e2cb(NpxProbeType type, int shank, int electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int, int[])
     */
    public static int[][] e2cb(NpxProbeType type, int shank, int[] electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int[], int[])
     */
    public static int[][] e2cb(NpxProbeType type, int[] shank, int[] electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * @see ChannelMapUtil#e2c0(int)
     */
    public static ChannelMapUtil.CB e2c0(int electrode) {
        var n = NpxProbeType.NP0.nChannel();
        return new ChannelMapUtil.CB(electrode % n, electrode / n);
    }

    /**
     * @see ChannelMapUtil#e2c0(int[])
     */
    public static int[][] e2c0(int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = e2c0(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2c21(int)
     */
    public static ChannelMapUtil.CB e2c21(int electrode) {
        var n = NpxProbeType.NP21.nChannel();
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

    /**
     * @see ChannelMapUtil#e2c21(int[])
     */
    public static int[][] e2c21(int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = e2c21(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2c24(int, int)
     */
    public static ChannelMapUtil.CB e2c24(int shank, int electrode) {
        var n = NpxProbeType.NP24.nChannel();
        var bank = electrode / n;
        var e1 = electrode % n;
        var b1 = e1 / 48/*type.nElectrodePerBlock()*/;
        var index = e1 % 48;
        var block = ELECTRODE_MAP_24[shank][b1];
        return new ChannelMapUtil.CB(48 * block + index, bank);
    }

    /**
     * @see ChannelMapUtil#e2c24(int, int[])
     */
    public static int[][] e2c24(int shank, int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = e2c24(shank, electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2c24(int[], int[])
     */
    public static int[][] e2c24(int[] shank, int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = e2c24(shank[i], electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

}
