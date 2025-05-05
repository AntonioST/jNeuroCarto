package io.ast.jneurocarto.probe_npx;

import java.util.Arrays;

public final class ChannelMapUtilPlain {

    private ChannelMapUtilPlain() {
        throw new RuntimeException();
    }

    static int[][] empty(int length) {
        var ret = new int[3][];
        ret[0] = new int[length];
        ret[1] = new int[length];
        ret[2] = new int[length];
        return ret;
    }

    static int[][] empty(int n, int length) {
        var ret = new int[n][];
        for (int i = 0; i < n; i++) {
            ret[i] = new int[length];
        }
        return ret;
    }

    static int check(int[][] a) {
        if (a.length != 3) throw new IllegalArgumentException();
        var length = a[0].length;
        if (a[1].length != length) throw new IllegalArgumentException();
        if (a[2].length != length) throw new IllegalArgumentException();
        return length;
    }

    static int[][] like(int[][] a, boolean cloneFirst) {
        var length = check(a);

        var ret = new int[3][];
        ret[0] = cloneFirst ? a[0].clone() : new int[length];
        ret[1] = new int[length];
        ret[2] = new int[length];

        return ret;
    }

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

    public static int[][] e2cr(NpxProbeType type, int[] electrode) {
        var nc = type.nColumnPerShank();

        var ret = empty(electrode.length);

        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[1][i] = electrode[i] % nc;
            ret[2][i] = electrode[i] / nc;
        }
        return ret;
    }

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

    public static int[] cr2e(NpxProbeType type, int[][] scr) {
        var ret = new int[check(scr)];

        var nc = type.nColumnPerShank();

        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = scr[1][i] + scr[2][i] * nc;
        }

        return ret;
    }

    public static int[] e2c(NpxProbeType type, int[][] scr) {
        var e = cr2e(type, scr);
        var cb = e2cb(type, scr[0], e);
        var n = type.nChannel();
        var ret = new int[e.length];
        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = cb[0][i] + cb[1][i] * n;
        }
        return ret;
    }

    public static int[][] e2cb(NpxProbeType type, int shank, int[] electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    public static int[][] e2cb(NpxProbeType type, int[] shank, int[] electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    public static int[][] e2c0(int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c0(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    public static int[][] e2c21(int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c21(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    public static int[][] e2c24(int shank, int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c24(shank, electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    public static int[][] e2c24(int[] shank, int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c24(shank[i], electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

}
