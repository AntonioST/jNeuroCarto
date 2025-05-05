package io.ast.jneurocarto.probe_npx;

import java.util.Arrays;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;


import static io.ast.jneurocarto.probe_npx.ChannelMapUtil.ELECTRODE_MAP_21;
import static io.ast.jneurocarto.probe_npx.ChannelMapUtilPlain.*;
import static jdk.incubator.vector.VectorOperators.FMA;

@SuppressWarnings("unused")
public final class ChannelMapUtilVec {

    private static final VectorSpecies<Integer> I = IntVector.SPECIES_PREFERRED;
    private static final int[] IDX;

    static {
        var a = new int[I.length()];
        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }
        IDX = a;
    }

    private ChannelMapUtilVec() {
        throw new RuntimeException();
    }

    public static int[][] electrodePosSCR(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();

        var ret = empty(ns * ne);

        int i = 0;
        for (int u = I.loopBound(ret[0].length); i < u; i += I.length()) {
            var ii = IntVector.fromArray(I, IDX, 0).add(i);

            var t1 = divmod(ii, ne);
            var s = t1.div();
            var v = t1.mod();

            var t2 = divmod(v, nc);
            var r = t2.div();
            var c = t2.mod();

            // shank
            s.intoArray(ret[0], i);
            // column
            c.intoArray(ret[1], i);
            // row
            r.intoArray(ret[2], i);
        }

        // shank
        for (; i < ns * ne; i++) {
            var s = i / ne;
            var v = i % ne;

            ret[0][i] = s;
            ret[1][i] = v % nc;
            ret[2][i] = v / nc;
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

        int i = 0;
        for (int u = I.loopBound(ret[0].length); i < u; i += I.length()) {
            var ii = IntVector.fromArray(I, IDX, 0).add(i);

            var t1 = divmod(ii, ne);
            var s = t1.div();
            var v = t1.mod();

            var t2 = divmod(v, nc);
            var r = t2.div();
            var c = t2.mod();

            // s
            s.intoArray(ret[0], i);
            // x
            s.mul(ps).add(c.mul(pc)).intoArray(ret[1], i);
            // y
            r.mul(pr).intoArray(ret[2], i);
        }

        for (; i < ns * ne; i++) {
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

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            IntVector.fromArray(I, cr[0], i).mul(pc).add(shank * ps).intoArray(ret[1], i);
            IntVector.fromArray(I, cr[1], i).mul(pr).intoArray(ret[2], i);
        }
        for (int length = electrode.length; i < length; i++) {
            ret[1][i] = cr[1][i] * pc + shank * ps;
            ret[2][i] = cr[2][i] * pr;
        }
        return ret;
    }

    public static int[][] e2cr(NpxProbeType type, int[] electrode) {
        var nc = type.nColumnPerShank();

        var ret = empty(electrode.length);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var t = divmod(e, nc);
            var r = t.div();
            var c = t.mod();
            c.intoArray(ret[1], i);
            r.intoArray(ret[2], i);
        }
        for (int length = electrode.length; i < length; i++) {
            ret[1][i] = electrode[i] % nc;
            ret[2][i] = electrode[i] / nc;
        }
        return ret;
    }

    public static int[][] e2xy(NpxProbeType type, int[][] scr) {
        var ret = like(scr, true);
        var length = ret[0].length;

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();

        int i = 0;
        for (int u = I.loopBound(length); i < u; i += I.length()) {
            var s = IntVector.fromArray(I, scr[0], i);
            var c = IntVector.fromArray(I, scr[1], i);
            var r = IntVector.fromArray(I, scr[2], i);
            c.mul(pc).add(s.mul(ps)).intoArray(ret[1], i);
            r.mul(pr).intoArray(ret[2], i);
        }
        for (; i < length; i++) {
            ret[1][i] = scr[1][i] * pc + scr[0][i] * ps;
            ret[2][i] = scr[2][i] * pr;
        }
        return ret;
    }

    public static int[] cr2e(NpxProbeType type, int[][] scr) {
        var ret = new int[check(scr)];

        var nc = type.nColumnPerShank();

        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var c = IntVector.fromArray(I, scr[1], i);
            var r = IntVector.fromArray(I, scr[2], i);
            r.lanewise(FMA, nc, c).intoArray(ret, i);
        }
        for (int length = ret.length; i < length; i++) {
            ret[i] = scr[1][i] + scr[2][i] * nc;
        }

        return ret;
    }

    public static int[] e2c(NpxProbeType type, int[][] scr) {
        var e = cr2e(type, scr);
        var cb = e2cb(type, scr[0], e);

        var n = type.nChannel();
        var ret = new int[e.length];

        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var c = IntVector.fromArray(I, cb[0], i);
            var b = IntVector.fromArray(I, cb[1], i);
            b.lanewise(FMA, n, c).intoArray(ret, i);
        }
        for (int length = ret.length; i < length; i++) {
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
        var n = NpxProbeType.NP1.nChannel();
        var ret = empty(2, electrode.length);
        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var t = divmod(e, n);
            var r = t.div();
            var c = t.mod();
            c.intoArray(ret[0], i);
            r.intoArray(ret[1], i);
        }
        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c21(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    public static int[][] e2c21(int[] electrode) {
        var n = NpxProbeType.NP21.nChannel();
        var bf = ELECTRODE_MAP_21[0];
        var ba = ELECTRODE_MAP_21[1];
        var ret = empty(2, electrode.length);
        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);

            var t1 = divmod(e, n);
            var bank = t1.div();
            bank.intoArray(ret[1], i);

            var t2 = divmod(t1.mod(), 32);
            var block = t2.div();

            var t3 = divmod(t2.mod(), 2);
            var row = t3.div();
            var column = t3.mod();

            var r = IntVector.fromArray(I, bf, 0, ret[1], i);
            var c = IntVector.fromArray(I, ba, 0, ret[1], i);
            var d = divmod(r.mul(c), 16).mod();
            var b = block.lanewise(FMA, 32, column);
            var channel = d.lanewise(FMA, 2, b);
            channel.intoArray(ret[0], i);
        }

        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c21(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    static final int[] ELECTRODE_MAP_24;

    static {
        var s = ChannelMapUtil.ELECTRODE_MAP_24;
        var n = s[0].length;
        var t = new int[s.length * n];
        for (int i = 0, length = t.length; i < length; i++) {
            t[i] = s[i / n][i % n];
        }
        ELECTRODE_MAP_24 = t;
    }

    public static int[][] e2c24(int shank, int[] electrode) {
        var n = NpxProbeType.NP24.nChannel();
        var s = ChannelMapUtil.ELECTRODE_MAP_24[shank];
        var ret = empty(2, electrode.length);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);

            var t1 = divmod(e, n);
            var bank = t1.div();
            bank.intoArray(ret[1], i);

            var t2 = divmod(t1.mod(), 48);
            var b = t2.div();
            var index = t2.mod();

            var block = IntVector.fromArray(I, s, 0, b.toArray(), 0);
            var channel = block.lanewise(FMA, 48, index);
            channel.intoArray(ret[0], i);
        }
        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c24(shank, electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }


    public static int[][] e2c24(int[] shank, int[] electrode) {
        var n = NpxProbeType.NP24.nChannel();
        var z = ELECTRODE_MAP_24.length / 4;
        var ret = empty(2, electrode.length);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var s = IntVector.fromArray(I, shank, i);

            var t1 = divmod(e, n);
            var bank = t1.div();
            bank.intoArray(ret[1], i);

            var t2 = divmod(t1.mod(), 48);
            var b1 = t2.div();
            var index = t2.mod();

            var b2 = s.lanewise(FMA, z, b1);
            var block = IntVector.fromArray(I, ELECTRODE_MAP_24, 0, b2.toArray(), 0);
            var channel = block.lanewise(FMA, 48, index);
            channel.intoArray(ret[0], i);
        }
        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c24(shank[i], electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    private record DivMod(IntVector div, IntVector mod) {
    }

    private static DivMod divmod(IntVector a, int b) {
        var c = a.div(b);
        var d = a.sub(c.mul(b));
        return new DivMod(c, d);
    }
}
