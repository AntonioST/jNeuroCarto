package io.ast.neurocarto.jmh;

import java.util.Arrays;

import io.ast.jneurocarto.probe_npx.ChannelMapUtil;
import io.ast.jneurocarto.probe_npx.NpxProbeType;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import static io.ast.neurocarto.jmh.ChannelMapUtilPlain.*;

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

    private static IntVector div(IntVector a, int b) {
        return a.div(b);
    }

    private static IntVector mod(IntVector a, int b) {
        return switch (b) {
            case 2 -> a.and(1);
            case 4 -> a.and(3);
            case 8 -> a.and(7);
            case 16 -> a.and(15);
            case 32 -> a.and(31);
            case 64 -> a.and(63);
            case 128 -> a.and(127);
            default -> {
                var c = a.div(b);
                yield a.sub(c.mul(b));

            }
        };
    }

    private static IntVector mod(IntVector a, IntVector b/* =a/c */, int c) {
        return a.sub(b.mul(c));
    }

    /**
     * @see ChannelMapUtil#electrodePosSCR(NpxProbeType)
     */
    public static int[][] electrodePosSCR(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();

        var ret = empty(ns * ne);

        int i = 0;
        for (int u = I.loopBound(ret[0].length); i < u; i += I.length()) {
            var ii = IntVector.fromArray(I, IDX, 0).add(i);

            var s = div(ii, ne);
            var v = mod(ii, s, ne);
            var r = div(v, nc);
            var c = mod(v, r, nc);

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

    /**
     * @see ChannelMapUtil#electrodePosXY(NpxProbeType)
     */
    public static int[][] electrodePosXY(NpxProbeType type) {
        var ns = type.nShank();
        var ne = type.nElectrodePerShank();
        var nc = type.nColumnPerShank();
        var ps = type.spacePerShank() * 10;
        var pc = (int) (type.spacePerColumn() * 10);
        var pr = (int) (type.spacePerRow() * 10);

        var ret = empty(ns * ne);

        int i = 0;
        for (int u = I.loopBound(ret[0].length); i < u; i += I.length()) {
            var ii = IntVector.fromArray(I, IDX, 0).add(i);

            var s = div(ii, ne);
            var v = mod(ii, s, ne);
            var r = div(v, nc);
            var c = mod(v, r, nc);

            // s
            s.intoArray(ret[0], i);
            // x
            s.mul(ps).add(c.mul(pc)).div(10).intoArray(ret[1], i);
            // y
            r.mul(pr).div(10).intoArray(ret[2], i);
        }

        for (; i < ns * ne; i++) {
            var s = i / ne;
            var v = i % ne;
            var r = v / nc;
            var c = v % nc;

            // s
            ret[0][i] = s;
            // x
            ret[1][i] = (s * ps + c * pc) / 10;
            // y
            ret[2][i] = (pr * r) / 10;
        }

        return ret;
    }

    /**
     * @see ChannelMapUtil#e2xy(NpxProbeType, int, int[])
     */
    public static int[][] e2xy(NpxProbeType type, int shank, int[] electrode) {
        var cr = e2cr(type, electrode);
        var ps = type.spacePerShank() * 10;
        var pc = (int) (type.spacePerColumn() * 10);
        var pr = (int) (type.spacePerRow() * 10);

        var ret = empty(electrode.length);
        Arrays.fill(ret[0], shank);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            IntVector.fromArray(I, cr[1], i).mul(pc).add(shank * ps).div(10).intoArray(ret[1], i);
            IntVector.fromArray(I, cr[2], i).mul(pr).div(10).intoArray(ret[2], i);
        }
        for (int length = electrode.length; i < length; i++) {
            ret[1][i] = (cr[1][i] * pc + shank * ps) / 10;
            ret[2][i] = (cr[2][i] * pr) / 10;
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#e2cr(NpxProbeType, int[])
     */
    public static int[][] e2cr(NpxProbeType type, int[] electrode) {
        var nc = type.nColumnPerShank();

        var ret = empty(electrode.length);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var r = div(e, nc);
            var c = mod(e, r, nc);
            c.intoArray(ret[1], i);
            r.intoArray(ret[2], i);
        }
        for (int length = electrode.length; i < length; i++) {
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
        var length = ret[0].length;

        var ps = type.spacePerShank() * 10;
        var pc = (int) (type.spacePerColumn() * 10);
        var pr = (int) (type.spacePerRow() * 10);

        int i = 0;
        for (int u = I.loopBound(length); i < u; i += I.length()) {
            var s = IntVector.fromArray(I, scr[0], i);
            var c = IntVector.fromArray(I, scr[1], i);
            var r = IntVector.fromArray(I, scr[2], i);
            c.mul(pc).add(s.mul(ps)).div(10).intoArray(ret[1], i);
            r.mul(pr).div(10).intoArray(ret[2], i);
        }
        for (; i < length; i++) {
            ret[1][i] = (scr[1][i] * pc + scr[0][i] * ps) / 10;
            ret[2][i] = (scr[2][i] * pr) / 10;
        }
        return ret;
    }

    /**
     * @see ChannelMapUtil#cr2e(NpxProbeType, int[][])
     */
    public static int[] cr2e(NpxProbeType type, int[][] scr) {
        var ret = new int[check3N(scr)];

        var nc = type.nColumnPerShank();

        int i = 0;
        for (int u = I.loopBound(ret.length); i < u; i += I.length()) {
            var c = IntVector.fromArray(I, scr[1], i);
            var r = IntVector.fromArray(I, scr[2], i);
            r.mul(nc).add(c).intoArray(ret, i);
        }
        for (int length = ret.length; i < length; i++) {
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

    /*=======================*
     * E-to-C/C-to-E mapping *
     *=======================*/

    public static int[] c2e(NpxProbeType type, int[][] channel, int shank) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> c2e21(channel);
            case NpxProbeType.NP24Base _ -> c2e24(channel, shank);
            case NpxProbeType.NP1110 _ -> c2e1110(channel);
            case NpxProbeType.NP2020 _ -> c2e2020(channel, shank);
            case NpxProbeType.NP3010 _ -> c2e3010(channel);
            case NpxProbeType.NP3020 _ -> c2e3020(channel, shank);
            default -> c2e0(channel);
        };
    }

    public static int[] c2e(NpxProbeType type, int[][] channel, int[] shank) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> c2e21(channel);
            case NpxProbeType.NP24Base _ -> c2e24(channel, shank);
            case NpxProbeType.NP1110 _ -> c2e1110(channel);
            case NpxProbeType.NP2020 _ -> c2e2020(channel, shank);
            case NpxProbeType.NP3010 _ -> c2e3010(channel);
            case NpxProbeType.NP3020 _ -> c2e3020(channel, shank);
            default -> c2e0(channel);
        };
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int, int[])
     */
    public static int[][] e2cb(NpxProbeType type, int shank, int[] electrode) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> e2c21(electrode);
            case NpxProbeType.NP24Base _ -> e2c24(shank, electrode);
            case NpxProbeType.NP1110 _ -> e2c1110(electrode);
            case NpxProbeType.NP2020 _ -> e2c2020(shank, electrode);
            case NpxProbeType.NP3010 _ -> e2c3010(electrode);
            case NpxProbeType.NP3020 _ -> e2c3020(shank, electrode);
            default -> e2c0(electrode);
        };
    }

    /**
     * @see ChannelMapUtil#e2cb(NpxProbeType, int[], int[])
     */
    public static int[][] e2cb(NpxProbeType type, int[] shank, int[] electrode) {
        return switch (type) {
            case NpxProbeType.NP21Base _ -> e2c21(electrode);
            case NpxProbeType.NP24Base _ -> e2c24(shank, electrode);
            case NpxProbeType.NP1110 _ -> e2c1110(electrode);
            case NpxProbeType.NP2020 _ -> e2c2020(shank, electrode);
            case NpxProbeType.NP3010 _ -> e2c3010(electrode);
            case NpxProbeType.NP3020 _ -> e2c3020(shank, electrode);
            default -> e2c0(electrode);
        };
    }

    /*===========================================*
     * E-to-C/C-to-E mapping for each probe type *
     *===========================================*/

    public static int[] c2e0(int[][] channels) {
        var n = NpxProbeType.np0.nChannel();
        int length = check2N(channels);
        var ret = new int[length];

        int i = 0;
        for (int u = I.loopBound(length); i < u; i += I.length()) {
            var c = IntVector.fromArray(I, channels[0], i);
            var b = IntVector.fromArray(I, channels[1], i);
            b.mul(n).add(mod(c, n)).intoArray(ret, i);
        }
        for (; i < length; i++) {
            ret[i] = ChannelMapUtilPlain.c2e0(channels[0][i], channels[1][i]);
        }
        return ret;
    }

    public static int[][] e2c0(int[] electrode) {
        var n = NpxProbeType.np0.nChannel();
        var ret = empty(2, electrode.length);
        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var r = div(e, n);
            var c = mod(e, r, n);
            c.intoArray(ret[0], i);
            r.intoArray(ret[1], i);
        }
        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtilPlain.e2c0(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    private static IntVector np21IndexOfRow(IntVector bank, IntVector row, IntVector col) {
        var buffer = new int[I.length()];
        bank.intoArray(buffer, 0);
        var bf = IntVector.fromArray(I, ELECTRODE_MAP_21[0], 0, buffer, 0);
        var ba = IntVector.fromArray(I, ELECTRODE_MAP_21[1], 0, buffer, 0);
        var result = IntVector.broadcast(I, -1);

        var colba = col.mul(ba);
        for (int r = 0; r < 16; r++) {
            var unset = result.compare(VectorOperators.EQ, -1);
            if (unset.anyTrue()) {
                var t1 = bf.mul(r).add(colba);
                var t2 = mod(t1, 16);
                var match = t2.compare(VectorOperators.EQ, row);
                result = result.blend(r, match.and(unset));
            } else {
                break;
            }
        }

        return result;
    }

    public static int[] c2e21(int[][] channels) {
        var n = NpxProbeType.np21.nChannel();

        int length = check2N(channels);
        var ret = new int[length];

        int i = 0;
        for (int u = I.loopBound(length); i < u; i += I.length()) {
            var c = IntVector.fromArray(I, channels[0], i);
            var b = IntVector.fromArray(I, channels[1], i);
            var block = div(c, 32);
            var index = mod(c, block, 32);
            var row = div(index, 2);
            var col = mod(index, row, 2);
            row = np21IndexOfRow(b, row, col);

            b.mul(n).add(block.mul(32)).add(row.mul(2)).add(col).intoArray(ret, i);
        }
        for (; i < length; i++) {
            ret[i] = ChannelMapUtilPlain.c2e21(channels[0][i], channels[1][i]);
        }
        return ret;
    }


    public static int[][] e2c21(int[] electrode) {
        var n = NpxProbeType.np21.nChannel();
        var bf = ELECTRODE_MAP_21[0];
        var ba = ELECTRODE_MAP_21[1];
        var ret = empty(2, electrode.length);
        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);

            var bank = e.div(n);
            bank.intoArray(ret[1], i);

            var e1 = mod(e, bank, n);
            var block = div(e1, 32);
            var e2 = mod(e1, block, 32);
            var row = div(e2, 2);
            var column = mod(e2, row, 2);

            // channel = 2 * ((row * bf[bank] + column * ba[bank]) % 16) + 32 * block + column;
            var f = IntVector.fromArray(I, bf, 0, ret[1], i);
            var a = IntVector.fromArray(I, ba, 0, ret[1], i);
            // channel = 2 * ((row * f + column * a) % 16) + 32 * block + column;
            var b16 = row.mul(f).add(column.mul(a));
            var b32 = block.mul(32);
            // channel = 2 * (b16 % 16) + b32 + column;
            var m16 = mod(b16, 16);
            // channel = 2 * m16 + b32 + column;
            var channel = m16.mul(2).add(b32).add(column);
            channel.intoArray(ret[0], i);
        }

        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtilPlain.e2c21(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    static final int[] ELECTRODE_MAP_24_FAT;

    static {
        var s = ELECTRODE_MAP_24;
        var n = s[0].length;
        var t = new int[s.length * n];
        for (int i = 0, length = t.length; i < length; i++) {
            t[i] = s[i / n][i % n];
        }
        ELECTRODE_MAP_24_FAT = t;
    }

    private static IntVector np24IndexOfBlock(IntVector shank, IntVector block) {
        var buffer = new int[I.length()];
        var s = shank.mul(8);
        var result = IntVector.broadcast(I, -1);

        for (int r = 0; r < 8; r++) {
            var unset = result.compare(VectorOperators.EQ, -1);
            if (unset.anyTrue()) {
                s.add(r).intoArray(buffer, 0);
                var b = IntVector.fromArray(I, ELECTRODE_MAP_24_FAT, 0, buffer, 0);
                var match = b.compare(VectorOperators.EQ, block);
                result = result.blend(r, match.and(unset));
            } else {
                break;
            }
        }

        return result;
    }

    public static int[] c2e24(int[][] channels, int shank) {
        int length = check2N(channels);
        var ret = new int[length];
        var s = IntVector.broadcast(I, shank);

        int i = 0;
        for (int u = I.loopBound(length); i < u; i += I.length()) {
            var c = IntVector.fromArray(I, channels[0], i);
            var b = IntVector.fromArray(I, channels[1], i);
            var block = div(c, 48);
            var index = mod(c, block, 48);
            block = np24IndexOfBlock(s, block);

            b.mul(384).add(block.mul(48)).add(index).intoArray(ret, i);
        }
        for (; i < length; i++) {
            ret[i] = ChannelMapUtilPlain.c2e24(channels[0][i], channels[1][i], shank);
        }
        return ret;
    }

    public static int[] c2e24(int[][] channels, int[] shank) {
        int length = check2N(channels);
        if (shank.length != length) throw new IllegalArgumentException();
        var ret = new int[length];

        int i = 0;
        for (int u = I.loopBound(length); i < u; i += I.length()) {
            var s = IntVector.fromArray(I, shank, i);
            var c = IntVector.fromArray(I, channels[0], i);
            var b = IntVector.fromArray(I, channels[1], i);
            var block = div(c, 48);
            var index = mod(c, block, 48);
            block = np24IndexOfBlock(s, block);

            b.mul(384).add(block.mul(48)).add(index).intoArray(ret, i);
        }
        for (; i < length; i++) {
            ret[i] = ChannelMapUtilPlain.c2e24(channels[0][i], channels[1][i], shank[i]);
        }
        return ret;
    }


    public static int[][] e2c24(int shank, int[] electrode) {
        var n = NpxProbeType.np24.nChannel();
        var s = ELECTRODE_MAP_24[shank];
        var ret = empty(2, electrode.length);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);

            var bank = div(e, n);
            var mod = mod(e, bank, n);
            bank.intoArray(ret[1], i);

            var b = div(mod, 48);
            var index = mod(mod, b, 48);

            var block = IntVector.fromArray(I, s, 0, b.toArray(), 0);
            var channel = block.mul(48).add(index);
            channel.intoArray(ret[0], i);
        }
        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtilPlain.e2c24(shank, electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }


    public static int[][] e2c24(int[] shank, int[] electrode) {
        var n = NpxProbeType.np24.nChannel();
        var z = ELECTRODE_MAP_24_FAT.length / 4;
        var ret = empty(2, electrode.length);

        int i = 0;
        for (int u = I.loopBound(electrode.length); i < u; i += I.length()) {
            var e = IntVector.fromArray(I, electrode, i);
            var s = IntVector.fromArray(I, shank, i);

            var bank = div(e, n);
            var mod = mod(e, bank, n);
            bank.intoArray(ret[1], i);

            var b1 = div(mod, 48);
            var index = mod(mod, b1, 48);

            var b2 = s.mul(z).add(b1);
            var block = IntVector.fromArray(I, ELECTRODE_MAP_24_FAT, 0, b2.toArray(), 0);
            var channel = block.mul(48).add(index);
            channel.intoArray(ret[0], i);
        }
        for (int length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtilPlain.e2c24(shank[i], electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    public static int[] c2e1110(int[][] channels) {
        //XXX Unsupported Operation ChannelMapUtilVec.c2e1110
        throw new UnsupportedOperationException();
    }

    public static int[][] e2c1110(int[] electrode) {
        //XXX Unsupported Operation ChannelMapUtilVec.e2c1110
        throw new UnsupportedOperationException();
    }

    public static int[] c2e2020(int[][] channels, int shank) {
        //XXX Unsupported Operation ChannelMapUtilVec.c2e2020
        throw new UnsupportedOperationException();
    }

    public static int[] c2e2020(int[][] channels, int[] shank) {
        //XXX Unsupported Operation ChannelMapUtilVec.c2e2020
        throw new UnsupportedOperationException();
    }

    public static int[][] e2c2020(int shank, int[] electrode) {
        //XXX Unsupported Operation ChannelMapUtilVec.e2c2020
        throw new UnsupportedOperationException();
    }

    public static int[][] e2c2020(int[] shank, int[] electrode) {
        //XXX Unsupported Operation ChannelMapUtilVec.e2c2020
        throw new UnsupportedOperationException();
    }

    public static int[] c2e3010(int[][] channels) {
        //XXX Unsupported Operation ChannelMapUtilVec.c2e3010
        throw new UnsupportedOperationException();
    }

    public static int[][] e2c3010(int[] electrode) {
        //XXX Unsupported Operation ChannelMapUtilVec.e2c3010
        throw new UnsupportedOperationException();
    }


    public static int[] c2e3020(int[][] channels, int shank) {
        //XXX Unsupported Operation ChannelMapUtilVec.c2e3020
        throw new UnsupportedOperationException();
    }

    public static int[] c2e3020(int[][] channels, int[] shank) {
        //XXX Unsupported Operation ChannelMapUtilVec.c2e3020
        throw new UnsupportedOperationException();
    }

    public static int[][] e2c3020(int shank, int[] electrode) {
        //XXX Unsupported Operation ChannelMapUtilVec.e2c3020
        throw new UnsupportedOperationException();
    }

    public static int[][] e2c3020(int[] shank, int[] electrode) {
        //XXX Unsupported Operation ChannelMapUtilVec.e2c3020
        throw new UnsupportedOperationException();
    }
}
