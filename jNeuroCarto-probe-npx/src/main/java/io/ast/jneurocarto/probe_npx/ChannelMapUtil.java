package io.ast.jneurocarto.probe_npx;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;

/// {@link ChannelMap} utilities.
///
/// ### Array annotation
///
/// * `E` number of electrode that equals to {@link NpxProbeType#nShank() nShank}*{@link NpxProbeType#nElectrodePerShank() nElectrodePerShank}
/// * `C` number of channels that equals to {@link NpxProbeType#nChannel() nChannel}
@NullMarked
public final class ChannelMapUtil {

    private ChannelMapUtil() {
        throw new RuntimeException();
    }

    /**
     * Get shank index for every channel.
     *
     * @param map           a channelmap
     * @param includeUnused include unused channels. Otherwise, -1 will put in the return.
     * @return int array[C]
     */
    public static int[] channelShank(ChannelMap map, boolean includeUnused) {
        var ret = new int[map.nChannel()];
        for (int i = 0, length = ret.length; i < length; i++) {
            if (map.getChannel(i) instanceof Electrode e && (e.inUsed || includeUnused)) {
                ret[i] = e.shank;
            } else {
                ret[i] = -1;
            }
        }
        return ret;
    }

    /**
     * Get column index for every channel.
     *
     * @param map           a channelmap
     * @param includeUnused include unused channels. Otherwise, -1 will put in the return.
     * @return int array[C]
     */
    public static int[] channelColumn(ChannelMap map, boolean includeUnused) {
        var ret = new int[map.nChannel()];
        for (int i = 0, length = ret.length; i < length; i++) {
            if (map.getChannel(i) instanceof Electrode e && (e.inUsed || includeUnused)) {
                ret[i] = e.column;
            } else {
                ret[i] = -1;
            }
        }
        return ret;
    }

    /**
     * Get row index for every channel.
     *
     * @param map           a channelmap
     * @param includeUnused include unused channels. Otherwise, -1 will put in the return.
     * @return int array[C]
     */
    public static int[] channelRow(ChannelMap map, boolean includeUnused) {
        var ret = new int[map.nChannel()];
        for (int i = 0, length = ret.length; i < length; i++) {
            if (map.getChannel(i) instanceof Electrode e && (e.inUsed || includeUnused)) {
                ret[i] = e.row;
            } else {
                ret[i] = -1;
            }
        }
        return ret;
    }

    /**
     * Get x position (um) for every channel.
     *
     * @param map           a channelmap
     * @param includeUnused include unused channels. Otherwise, -1 will put in the return.
     * @return int array[C]
     */
    public static int[] channelPosX(ChannelMap map, boolean includeUnused) {
        var pc = map.type().spacePerColumn();
        var ps = map.type().spacePerShank();

        var ret = new int[map.nChannel()];
        for (int i = 0, length = ret.length; i < length; i++) {
            if (map.getChannel(i) instanceof Electrode e && (e.inUsed || includeUnused)) {
                ret[i] = e.column * pc + e.shank * ps;
            } else {
                ret[i] = -1;
            }
        }
        return ret;
    }

    /**
     * Get y position (um) for every channel.
     *
     * @param map           a channelmap
     * @param includeUnused include unused channels. Otherwise, -1 will put in the return.
     * @return int array[C]
     */
    public static int[] channelPosY(ChannelMap map, boolean includeUnused) {
        var pr = map.type().spacePerRow();

        var ret = new int[map.nChannel()];
        for (int i = 0, length = ret.length; i < length; i++) {
            if (map.getChannel(i) instanceof Electrode e && (e.inUsed || includeUnused)) {
                ret[i] = e.row * pr;
            } else {
                ret[i] = -1;
            }
        }
        return ret;
    }

    /**
     * New empty array.
     *
     * @param length
     * @return int array[3][{@code length}]
     */
    private static int[][] empty(int length) {
        var ret = new int[3][];
        ret[0] = new int[length];
        ret[1] = new int[length];
        ret[2] = new int[length];
        return ret;
    }

    /**
     * New empty array.
     *
     * @param n
     * @param length
     * @return int array[{@code n}][{@code length}]
     */
    private static int[][] empty(int n, int length) {
        var ret = new int[n][];
        for (int i = 0; i < n; i++) {
            ret[i] = new int[length];
        }
        return ret;
    }

    /**
     * Check array has equal length.
     *
     * @param a 2-d matric-like array
     * @return the length of the second axis.
     */
    private static int check(int[][] a) {
        if (a.length != 3) throw new IllegalArgumentException();
        var length = a[0].length;
        if (a[1].length != length) throw new IllegalArgumentException();
        if (a[2].length != length) throw new IllegalArgumentException();
        return length;
    }

    /**
     * New array with same size of the input.
     *
     * @param a          2-d matric-like array
     * @param cloneFirst clone the first row ({@code a[0]}).
     * @return an int array with same size.
     */
    private static int[][] like(int[][] a, boolean cloneFirst) {
        var length = check(a);

        var ret = new int[3][];
        ret[0] = cloneFirst ? a[0].clone() : new int[length];
        ret[1] = new int[length];
        ret[2] = new int[length];

        return ret;
    }

    /**
     * Get shank, column, and row index array for every electrode.
     *
     * @param type neuropixels probe type.
     * @return int array [(shank, column, row)][E]
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
     * Get shank, x and y position array for every electrode.
     *
     * @param type neuropixels probe type.
     * @return int array [(shank, x, y)][E]
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

    public record XY(int s, int x, int y) {
    }

    public record CR(int s, int c, int r) {
    }

    public record CB(int channel, int bank) {
    }

    public static XY e2xy(NpxProbeType type, int electrode) {
        var cr = e2cr(type, electrode);
        return new XY(
          0,
          cr.c() * type.spacePerColumn(),
          cr.r() * type.spacePerRow()
        );
    }

    /**
     * Get shank, x and y position array for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index array [E]
     * @return int array [(shank, x, y)][E]
     */
    public static int[][] e2xy(NpxProbeType type, int[] electrode) {
        return e2xy(type, 0, electrode);
    }

    /**
     * Get shank, x and y position array for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param shank     given shank
     * @param electrode electrode index
     * @return index result.
     */
    public static XY e2xy(NpxProbeType type, int shank, int electrode) {
        var cr = e2cr(type, electrode);
        return new XY(
          shank,
          shank * type.spacePerShank() + cr.c() * type.spacePerColumn(),
          cr.r() * type.spacePerRow()
        );
    }

    /**
     * Get shank, x and y position array for given electrodes.
     *
     * @param type      neuropixels probe type.
     * @param shank     given shank
     * @param electrode electrode index array [E]
     * @return int array [(shank, x, y)][E]
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
     * Get shank, x and y position array for given electrode position.
     *
     * @param type   neuropixels probe type.
     * @param shank  given shank
     * @param column column index
     * @param row    row index
     * @return index result.
     */
    public static XY e2xy(NpxProbeType type, int shank, int column, int row) {
        return new XY(
          shank,
          shank * type.spacePerShank() + column * type.spacePerColumn(),
          row * type.spacePerRow()
        );
    }

    /**
     * Get shank, x and y position array for given electrode positions.
     *
     * @param type neuropixels probe type.
     * @param scr  int array [(shank, column, row)][E]
     * @return int array [(shank, x, y)][E]
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
     * Get shank, x and y position array for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode given electrode
     * @return index result.
     */
    public static XY e2xy(NpxProbeType type, Electrode electrode) {
        return new XY(
          electrode.shank,
          electrode.shank * type.spacePerShank() + electrode.column * type.spacePerColumn(),
          electrode.row * type.spacePerRow()
        );
    }

    /**
     * Get shank, column and row index array for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index
     * @return index result.
     */
    public static CR e2cr(NpxProbeType type, int electrode) {
        var nc = type.nColumnPerShank();
        return new CR(
          0,
          electrode % nc,
          electrode / nc
        );
    }

    /**
     * Get shank, column and row index array for given electrodes.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index array [E]
     * @return int array[(shank, column, row)][E].
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
     * Get shank, column and row index array for given electrode position.
     *
     * @param type   neuropixels probe type.
     * @param shank  given shank
     * @param column column index
     * @param row    row index
     * @return int result
     */
    public static CR e2cr(NpxProbeType type, int shank, int column, int row) {
        return new CR(shank, column, row);
    }

    /**
     * Get shank, column and row index array for given electrode positions.
     *
     * @param type neuropixels probe type.
     * @param scr  int array [(shank, column, row)][E]
     * @return int result
     */
    public static int[][] e2cr(NpxProbeType type, int[][] scr) {
        check(scr);
        return scr;
    }

    /**
     * Get shank, column and row index array for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode given electrode
     * @return int result
     */
    public static CR e2cr(NpxProbeType type, Electrode electrode) {
        return new CR(electrode.shank, electrode.column, electrode.row);
    }

    /**
     * Get electrode index for given electrode position.
     *
     * @param type   neuropixels probe type.
     * @param column column index
     * @param row    row index
     * @return electrode index
     */
    public static int cr2e(NpxProbeType type, int column, int row) {
        var nc = type.nColumnPerShank();
        return column + nc * row;
    }

    /**
     * Get electrode index for given electrode positions.
     *
     * @param type neuropixels probe type.
     * @param scr  int array [(shank, column, row)][E]
     * @return electrode index array[E].
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
     * Get electrode index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode given electrode
     * @return electrode index
     */
    public static int cr2e(NpxProbeType type, Electrode electrode) {
        var nc = type.nColumnPerShank();
        return electrode.column + nc * electrode.row;
    }

    /**
     * Get channel index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index
     * @return channel index
     */
    public static int e2c(NpxProbeType type, int electrode) {
        return e2cb(type, electrode).channel;
    }

    /**
     * Get channel index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index
     * @return channel index
     */
    public static int e2c(NpxProbeType type, int shank, int electrode) {
        var cb = e2cb(type, shank, electrode);
        return cb.channel;
    }

    /**
     * Get channel index for given electrode positions.
     *
     * @param type neuropixels probe type.
     * @param scr  int array [(shank, column, row)][E]
     * @return channel index array[E]
     */
    public static int[] e2c(NpxProbeType type, int[][] scr) {
        var e = cr2e(type, scr);
        var cb = e2cb(type, scr[0], e);
        return cb[0];
    }

    /**
     * Get channel index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode given electrode
     * @return channel index
     */
    public static int e2c(NpxProbeType type, Electrode electrode) {
        return e2cb(type, electrode).channel;
    }

    /**
     * Get channel and bank index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index
     * @return index result
     */
    public static CB e2cb(NpxProbeType type, int electrode) {
        return e2cb(type, 0, electrode);
    }

    /**
     * Get channel and bank index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param shank     shank index
     * @param electrode electrode index
     * @return index result
     */
    public static CB e2cb(NpxProbeType type, int shank, int electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Get channel and bank index for given electrode position.
     *
     * @param type   neuropixels probe type.
     * @param shank  shank index
     * @param column column index
     * @param row    row index
     * @return index result
     */
    public static CB e2cb(NpxProbeType type, int shank, int column, int row) {
        return e2cb(type, shank, cr2e(type, column, row));
    }

    /**
     * Get channel and bank index for given electrodes.
     *
     * @param type      neuropixels probe type.
     * @param electrode electrode index array [E]
     * @return index array [(channel, bank)][E]
     */
    public static int[][] e2cb(NpxProbeType type, int[] electrode) {
        return e2cb(type, 0, electrode);
    }

    /**
     * Get channel and bank index for given electrodes on shank.
     *
     * @param type      neuropixels probe type.
     * @param shank     shank index
     * @param electrode electrode index array [E]
     * @return index array [(channel, bank)][E]
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
     * Get channel and bank index for given electrodes on shank.
     *
     * @param type      neuropixels probe type.
     * @param shank     shank index array[E]
     * @param electrode electrode index array [E]
     * @return index array [(channel, bank)][E]
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
     * Get channel and bank index for given electrode.
     *
     * @param type      neuropixels probe type.
     * @param electrode given electrode
     * @return index result
     */
    public static CB e2cb(NpxProbeType type, Electrode electrode) {
        return e2cb(type, electrode.shank, cr2e(type, electrode));
    }

    /**
     * Get channel and bank index for given electrode for {@link NpxProbeType#NP0}.
     *
     * @param electrode electrode index
     * @return index result
     */
    public static CB e2c0(int electrode) {
        var n = NpxProbeType.NP0.nChannel();
        return new CB(electrode % n, electrode / n);
    }

    /**
     * Get channel and bank index for given electrodes for {@link NpxProbeType#NP0}.
     *
     * @param electrode electrode index array[E]
     * @return index array [(channel, bank)][E]
     */
    public static int[][] e2c0(int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c0(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    /**
     * Get channel and bank index for given electrode for {@link NpxProbeType#NP21}.
     *
     * @param electrode electrode index
     * @return index result
     */
    public static CB e2c21(int electrode) {
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
        return new CB(channel, bank);
    }

    /**
     * Get channel and bank index for given electrodes for {@link NpxProbeType#NP21}.
     *
     * @param electrode electrode index array[E]
     * @return index array [(channel, bank)][E]
     */
    public static int[][] e2c21(int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c21(electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    /**
     * Get channel and bank index for given electrode for {@link NpxProbeType#NP24}.
     *
     * @param shank     shank index
     * @param electrode electrode index
     * @return index result
     */
    public static CB e2c24(int shank, int electrode) {
        var n = NpxProbeType.NP24.nChannel();
        var bank = electrode / n;
        var e1 = electrode % n;
        var b1 = e1 / 48/*type.nElectrodePerBlock()*/;
        var index = e1 % 48;
        var block = ELECTRODE_MAP_24[shank][b1];
        return new CB(48 * block + index, bank);
    }

    /**
     * Get channel and bank index for given electrodes for {@link NpxProbeType#NP24}.
     *
     * @param electrode electrode index array[E]
     * @return index array [(channel, bank)][E]
     */
    public static int[][] e2c24(int shank, int[] electrode) {
        var ret = empty(2, electrode.length);
        for (int i = 0, length = electrode.length; i < length; i++) {
            var tmp = ChannelMapUtil.e2c24(shank, electrode[i]);
            ret[0][i] = tmp.channel();
            ret[1][i] = tmp.bank();
        }
        return ret;
    }

    /**
     * Get channel and bank index for given electrodes for {@link NpxProbeType#NP24}.
     *
     * @param shank     shank index array[E]
     * @param electrode electrode index array[E]
     * @return index array [(channel, bank)][E]
     */
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
