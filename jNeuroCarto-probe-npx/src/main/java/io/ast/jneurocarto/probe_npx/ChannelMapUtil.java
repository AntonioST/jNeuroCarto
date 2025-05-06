package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ChannelMapUtil {

    private static final boolean USE_VECTOR = !System.getProperty("io.ast.jneurocarto.probe_npx.use_vector", "").isEmpty();

    private ChannelMapUtil() {
        throw new RuntimeException();
    }

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
     * @param type
     * @return {0: shank, 1: column, 2: row} by int[nShank*nElectrodePerShank]
     */
    public static int[][] electrodePosSCR(NpxProbeType type) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.electrodePosSCR(type);
        } else {
            return ChannelMapUtilPlain.electrodePosSCR(type);
        }
    }


    /**
     * @param type
     * @return {0: shank, 1: x, 2: y} by int[nShank*nElectrodePerShank]
     */
    public static int[][] electrodePosXY(NpxProbeType type) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.electrodePosXY(type);
        } else {
            return ChannelMapUtilPlain.electrodePosXY(type);
        }
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

    public static int[][] e2xy(NpxProbeType type, int[] electrode) {
        return e2xy(type, 0, electrode);
    }

    public static XY e2xy(NpxProbeType type, int shank, int electrode) {
        var cr = e2cr(type, electrode);
        return new XY(
          shank,
          shank * type.spacePerShank() + cr.c() * type.spacePerColumn(),
          cr.r() * type.spacePerRow()
        );
    }

    public static int[][] e2xy(NpxProbeType type, int shank, int[] electrode) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2xy(type, shank, electrode);
        } else {
            return ChannelMapUtilPlain.e2xy(type, shank, electrode);
        }
    }

    public static XY e2xy(NpxProbeType type, int shank, int column, int row) {
        return new XY(
          shank,
          shank * type.spacePerShank() + column * type.spacePerColumn(),
          row * type.spacePerRow()
        );
    }

    public static int[][] e2xy(NpxProbeType type, int[][] scr) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2xy(type, scr);
        } else {
            return ChannelMapUtilPlain.e2xy(type, scr);
        }
    }

    public static XY e2xy(NpxProbeType type, Electrode electrode) {
        return new XY(
          electrode.shank,
          electrode.shank * type.spacePerShank() + electrode.column * type.spacePerColumn(),
          electrode.row * type.spacePerRow()
        );
    }

    public static CR e2cr(NpxProbeType type, int electrode) {
        var nc = type.nColumnPerShank();
        return new CR(
          0,
          electrode % nc,
          electrode / nc
        );
    }

    public static int[][] e2cr(NpxProbeType type, int[] electrode) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2cr(type, electrode);
        } else {
            return ChannelMapUtilPlain.e2cr(type, electrode);
        }
    }

    public static CR e2cr(NpxProbeType type, int shank, int column, int row) {
        return new CR(shank, column, row);
    }

    public static int[][] e2cr(NpxProbeType type, int[][] scr) {
        return scr;
    }

    public static CR e2cr(NpxProbeType type, Electrode electrode) {
        return new CR(electrode.shank, electrode.column, electrode.row);
    }

    public static int cr2e(NpxProbeType type, int column, int row) {
        var nc = type.nColumnPerShank();
        return column + nc * row;
    }

    public static int[] cr2e(NpxProbeType type, int[][] scr) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.cr2e(type, scr);
        } else {
            return ChannelMapUtilPlain.cr2e(type, scr);
        }
    }

    public static int cr2e(NpxProbeType type, Electrode electrode) {
        var nc = type.nColumnPerShank();
        return electrode.column + nc * electrode.row;
    }

    public static int e2c(NpxProbeType type, int electrode) {
        return e2cb(type, electrode).channel;
    }

    public static int e2c(NpxProbeType type, int shank, int electrode) {
        var cb = e2cb(type, shank, electrode);
        return cb.channel;
    }

    public static int[] e2c(NpxProbeType type, int[][] scr) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2c(type, scr);
        } else {
            return ChannelMapUtilPlain.e2c(type, scr);
        }
    }

    public static int e2c(NpxProbeType type, Electrode electrode) {
        return e2cb(type, electrode).channel;
    }

    public static CB e2cb(NpxProbeType type, int electrode) {
        return e2cb(type, 0, electrode);
    }

    public static CB e2cb(NpxProbeType type, int shank, int electrode) {
        return switch (type.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    public static CB e2cb(NpxProbeType type, int shank, int column, int row) {
        return e2cb(type, shank, cr2e(type, column, row));
    }

    public static int[][] e2cb(NpxProbeType type, int[] electrode) {
        return e2cb(type, 0, electrode);
    }

    public static int[][] e2cb(NpxProbeType type, int shank, int[] electrode) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2cb(type, shank, electrode);
        } else {
            return ChannelMapUtilPlain.e2cb(type, shank, electrode);
        }
    }

    public static int[][] e2cb(NpxProbeType type, int[] shank, int[] electrode) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2cb(type, shank, electrode);
        } else {
            return ChannelMapUtilPlain.e2cb(type, shank, electrode);
        }
    }

    public static CB e2cb(NpxProbeType type, Electrode electrode) {
        return e2cb(type, electrode.shank, cr2e(type, electrode));
    }

    public static CB e2c0(int electrode) {
        var n = NpxProbeType.NP0.nChannel();
        return new CB(electrode % n, electrode / n);
    }

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

    public static CB e2c24(int shank, int electrode) {
        var n = NpxProbeType.NP24.nChannel();
        var bank = electrode / n;
        var e1 = electrode % n;
        var b1 = e1 / 48/*type.nElectrodePerBlock()*/;
        var index = e1 % 48;
        var block = ELECTRODE_MAP_24[shank][b1];
        return new CB(48 * block + index, bank);
    }
}
