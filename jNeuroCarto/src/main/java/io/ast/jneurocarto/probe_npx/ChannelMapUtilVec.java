package io.ast.jneurocarto.probe_npx;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

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

    public static int[][] electrodePosSCR(NpxProbeInfo info) {
        var ns = info.nShank();
        var ne = info.nElectrodePerShank();
        var nc = info.nColumnPerShank();

        var ret = new int[3][ns * ne];

        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var ii = IntVector.fromArray(I, IDX, 0).add(i);

            var s = ii.div(ne);
            var v = ii.sub(s.mul(ne));
            var r = v.div(nc);
            var c = v.sub(r.mul(nc));

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


    public static int[][] electrodePosXY(NpxProbeInfo info) {
        var ns = info.nShank();
        var ne = info.nElectrodePerShank();
        var nc = info.nColumnPerShank();
        var ps = info.spacePerShank();
        var pc = info.spacePerColumn();
        var pr = info.spacePerRow();

        var ret = new int[2][ns * ne];

        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var ii = IntVector.fromArray(I, IDX, 0).add(i);

            // shank
            var s = ii.div(ne);
            var v = ii.sub(s.mul(ne));
            var r = v.div(nc);
            var c = v.sub(r.mul(nc));

            // x
            s.mul(ps).add(c.mul(pc)).intoArray(ret[0], i);
            // y
            r.mul(pr).intoArray(ret[1], i);
        }

        for (; i < ns * ne; i++) {
            var s = i / ne;
            var v = i % ne;
            var r = v / nc;
            var c = v % nc;

            // x
            ret[0][i] = s * ps + c * pc;
            // y
            ret[1][i] = pr * r;
        }

        return ret;
    }

    public static int[][] e2xy(NpxProbeInfo info, int shank, int[] electrode) {
        var cr = e2cr(info, electrode);
        var ps = info.spacePerShank();
        var pc = info.spacePerColumn();
        var pr = info.spacePerRow();
        var ret = new int[2][electrode.length];
        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            IntVector.fromArray(I, cr[0], i).mul(pc).add(shank * ps).intoArray(ret[0], i);
            IntVector.fromArray(I, cr[1], i).mul(pr).intoArray(ret[1], i);
        }
        for (int length = electrode.length; i < length; i++) {
            ret[0][i] = cr[0][i] * pc + shank * ps;
            ret[1][i] = cr[1][i] * pr;
        }
        return ret;
    }

    public static int[][] e2cr(NpxProbeInfo info, int[] electrode) {
        var nc = info.nColumnPerShank();
        var ret = new int[2][electrode.length];
        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var r = e.div(nc);
            var c = e.sub(r.mul(nc));
            c.intoArray(ret[0], i);
            r.intoArray(ret[1], i);
        }
        for (int length = electrode.length; i < length; i++) {
            ret[0][i] = electrode[i] % nc;
            ret[1][i] = electrode[i] / nc;
        }
        return ret;
    }

    public static int[][] e2xy(NpxProbeInfo info, int shank, int[][] cr) {
        if (cr.length != 2) throw new IllegalArgumentException();
        var length = cr[0].length;
        if (cr[1].length != length) throw new IllegalArgumentException();

        var ps = info.spacePerShank();
        var pc = info.spacePerColumn();
        var pr = info.spacePerRow();
        var ret = new int[2][length];
        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            IntVector.fromArray(I, cr[0], i).mul(pc).add(shank * ps).intoArray(ret[0], i);
            IntVector.fromArray(I, cr[1], i).mul(pr).intoArray(ret[1], i);
        }
        for (; i < length; i++) {
            ret[0][i] = cr[0][i] * pc + shank * ps;
            ret[1][i] = cr[1][i] * pr;
        }
        return ret;
    }

    public static int[][] e2xy(NpxProbeInfo info, int[][] scr) {
        if (scr.length != 3) throw new IllegalArgumentException();
        var length = scr[0].length;
        if (scr[1].length != length) throw new IllegalArgumentException();
        if (scr[2].length != length) throw new IllegalArgumentException();

        var ps = info.spacePerShank();
        var pc = info.spacePerColumn();
        var pr = info.spacePerRow();
        var ret = new int[2][length];
        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var s = IntVector.fromArray(I, scr[0], i);
            var c = IntVector.fromArray(I, scr[1], i);
            var r = IntVector.fromArray(I, scr[2], i);
            c.mul(pc).add(s.mul(ps)).intoArray(ret[0], i);
            r.mul(pr).intoArray(ret[1], i);
        }
        for (; i < length; i++) {
            ret[0][i] = scr[1][i] * pc + scr[0][i] * ps;
            ret[1][i] = scr[2][i] * pr;
        }
        return ret;
    }
}
