package io.ast.jneurocarto.probe_npx;

@SuppressWarnings("unused")
public final class ChannelMapUtil {

    private static final boolean USE_VECTOR = !System.getProperty("io.ast.jneurocarto.use_vector").isEmpty();

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
        var pc = map.info().spacePerColumn();
        var ps = map.info().spacePerShank();

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
        var pr = map.info().spacePerRow();

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

    public static int[][] electrodePosSCR(NpxProbeInfo info) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.electrodePosSCR(info);
        } else {
            return ChannelMapUtilPlain.electrodePosSCR(info);
        }
    }


    public static int[][] electrodePosXY(NpxProbeInfo info) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.electrodePosXY(info);
        } else {
            return ChannelMapUtilPlain.electrodePosXY(info);
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

    public record XY(int x, int y) {
    }

    public record CR(int c, int r) {
    }

    public record CB(int channel, int bank) {
    }

    public static XY e2xy(NpxProbeInfo info, int electrode) {
        var cr = e2cr(info, electrode);
        return new XY(
          cr.c() * info.spacePerColumn(),
          cr.r() * info.spacePerRow()
        );
    }

    public static int[][] e2xy(NpxProbeInfo info, int[] electrode) {
        return e2xy(info, 0, electrode);
    }

    public static XY e2xy(NpxProbeInfo info, int shank, int electrode) {
        var cr = e2cr(info, electrode);
        return new XY(
          shank * info.spacePerShank() + cr.c() * info.spacePerColumn(),
          cr.r() * info.spacePerRow()
        );
    }

    public static int[][] e2xy(NpxProbeInfo info, int shank, int[] electrode) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2xy(info, shank, electrode);
        } else {
            return ChannelMapUtilPlain.e2xy(info, shank, electrode);
        }
    }

    public static XY e2xy(NpxProbeInfo info, int shank, int column, int row) {
        return new XY(
          shank * info.spacePerShank() + column * info.spacePerColumn(),
          row * info.spacePerRow()
        );
    }

    public static int[][] e2xy(NpxProbeInfo info, int shank, int[][] cr) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2xy(info, shank, cr);
        } else {
            return ChannelMapUtilPlain.e2xy(info, shank, cr);
        }
    }

    public static int[][] e2xy(NpxProbeInfo info, int[][] scr) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2xy(info, scr);
        } else {
            return ChannelMapUtilPlain.e2xy(info, scr);
        }
    }

    public static XY e2xy(NpxProbeInfo info, Electrode electrode) {
        return new XY(
          electrode.shank * info.spacePerShank() + electrode.column * info.spacePerColumn(),
          electrode.row * info.spacePerRow()
        );
    }

    public static XY e2xy(NpxProbeInfo info, int shank, Electrode electrode) {
        return new XY(
          shank * info.spacePerShank() + electrode.column * info.spacePerColumn(),
          electrode.row * info.spacePerRow()
        );
    }

    public static CR e2cr(NpxProbeInfo info, int electrode) {
        var nc = info.nColumnPerShank();
        return new CR(
          electrode % nc,
          electrode / nc
        );
    }

    public static int[][] e2cr(NpxProbeInfo info, int[] electrode) {
        if (USE_VECTOR) {
            return ChannelMapUtilVec.e2cr(info, electrode);
        } else {
            return ChannelMapUtilPlain.e2cr(info, electrode);
        }
    }

    public static CR e2cr(NpxProbeInfo info, int shank, int column, int row) {
        return new CR(column, row);
    }

    public static int[][] e2cr(NpxProbeInfo info, int[][] cr) {
        return cr;
    }

    public static CR e2cr(NpxProbeInfo info, Electrode electrode) {
        return new CR(electrode.column, electrode.row);
    }

    public static int cr2e(NpxProbeInfo info, int column, int row) {
        var nc = info.nColumnPerShank();
        return column + nc * row;
    }

    public static int cr2e(NpxProbeInfo info, Electrode electrode) {
        var nc = info.nColumnPerShank();
        return electrode.column + nc * electrode.row;
    }

    public static int e2c(NpxProbeInfo info, int electrode) {
        var cb = e2cb(info, electrode);
        var n = info.nChannel();
        return cb.channel + cb.bank * n;
    }

    public static int e2c(NpxProbeInfo info, int shank, int electrode) {
        var cb = e2cb(info, shank, electrode);
        var n = info.nChannel();
        return cb.channel + cb.bank * n;
    }

    public static CB e2cb(NpxProbeInfo info, int electrode) {
        return e2cb(info, 0, electrode);
    }

    public static CB e2cb(NpxProbeInfo info, int shank, int electrode) {
        return switch (info.code()) {
            case 0 -> e2c0(electrode);
            case 21 -> e2c21(electrode);
            case 24 -> e2c24(shank, electrode);
            default -> throw new IllegalArgumentException();
        };
    }

    public static CB e2cb(NpxProbeInfo info, int shank, int column, int row) {
        return e2cb(info, shank, cr2e(info, column, row));
    }

    public static CB e2cb(NpxProbeInfo info, Electrode electrode) {
        return e2cb(info, 0, cr2e(info, electrode));
    }

    public static CB e2cb(NpxProbeInfo info, int shank, Electrode electrode) {
        return e2cb(info, shank, cr2e(info, electrode));
    }

    public static CB e2c0(int electrode) {
        var n = NpxProbeType.NP1.info().nChannel();
        return new CB(electrode % n, electrode / n);
    }

    public static CB e2c21(int electrode) {
        var n = NpxProbeType.NP21.info().nChannel();
        var bf = ELECTRODE_MAP_21[0];
        var ba = ELECTRODE_MAP_21[0];
        var bank = electrode / n;
        var e1 = electrode % n;
        var block = e1 / 32/*info.nElectrodePerBlock()*/;
        var e2 = e1 % 32;
        var row = e2 / 2;
        var column = e2 % 2;
        var channel = 2 * ((row * bf[bank] + column * ba[bank]) % 16) + 32 * block + column;
        return new CB(channel, bank);
    }

    public static CB e2c24(int shank, int electrode) {
        var n = NpxProbeType.NP24.info().nChannel();
        var bank = electrode / n;
        var e1 = electrode % n;
        var b1 = e1 / 48/*info.nElectrodePerBlock()*/;
        var index = e1 % 48;
        var block = ELECTRODE_MAP_24[shank][b1];
        return new CB(48 * block + index, bank);
    }

}
