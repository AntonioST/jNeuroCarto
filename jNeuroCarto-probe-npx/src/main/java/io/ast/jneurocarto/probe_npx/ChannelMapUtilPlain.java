package io.ast.jneurocarto.probe_npx;

@SuppressWarnings("unused")
public final class ChannelMapUtilPlain {

    private ChannelMapUtilPlain() {
        throw new RuntimeException();
    }


    public static int[][] electrodePosSCR(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();

        var ret = new int[3][ns * ne];

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

        var ret = new int[2][ns * ne];

        for (int i = 0; i < ns * ne; i++) {
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

    public static int[][] e2xy(NpxProbeType type, int shank, int[] electrode) {
        var cr = e2cr(type, electrode);
        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();
        var ret = new int[2][electrode.length];
        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[0][i] = cr[0][i] * pc + shank * ps;
            ret[1][i] = cr[1][i] * pr;
        }
        return ret;
    }

    public static int[][] e2cr(NpxProbeType type, int[] electrode) {
        var nc = type.nColumnPerShank();
        var ret = new int[2][electrode.length];
        for (int i = 0, length = electrode.length; i < length; i++) {
            ret[0][i] = electrode[i] % nc;
            ret[1][i] = electrode[i] / nc;
        }
        return ret;
    }

    public static int[][] e2xy(NpxProbeType type, int shank, int[][] cr) {
        if (cr.length != 2) throw new IllegalArgumentException();
        var length = cr[0].length;
        if (cr[1].length != length) throw new IllegalArgumentException();

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();
        var ret = new int[2][length];
        for (int i = 0; i < length; i++) {
            ret[0][i] = cr[0][i] * pc + shank * ps;
            ret[1][i] = cr[1][i] * pr;
        }
        return ret;
    }

    public static int[][] e2xy(NpxProbeType type, int[][] scr) {
        if (scr.length != 3) throw new IllegalArgumentException();
        var length = scr[0].length;
        if (scr[1].length != length) throw new IllegalArgumentException();
        if (scr[2].length != length) throw new IllegalArgumentException();

        var ps = type.spacePerShank();
        var pc = type.spacePerColumn();
        var pr = type.spacePerRow();
        var ret = new int[2][length];
        for (int i = 0; i < length; i++) {
            ret[0][i] = scr[1][i] * pc + scr[0][i] * ps;
            ret[1][i] = scr[2][i] * pr;
        }
        return ret;
    }


}
