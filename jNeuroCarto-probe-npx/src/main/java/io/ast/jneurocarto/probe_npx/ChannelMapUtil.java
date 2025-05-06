package io.ast.jneurocarto.probe_npx;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static ChannelMap npx24SingleShank(int shank, double row) {
        var type = NpxProbeType.NP24;
        try {
            return npx24SingleShank(shank, (int) (row / type.spacePerRow()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("row over range : ")) {
                throw new IllegalArgumentException("row over range : " + row + " um", e);
            } else {
                throw e;
            }
        }
    }

    public static ChannelMap npx24SingleShank(int shank, int row) {
        var type = NpxProbeType.NP24;
        if (!(0 <= shank && shank < type.nShank())) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var nc = type.nColumnPerShank();
        var nr = type.nChannel() / nc;
        if (!(0 <= row && row + nr < type.nRowPerShank())) {
            throw new IllegalArgumentException("row over range : " + row);
        }

        var ret = new ChannelMap(type);
        for (int r = 0; r < nr; r++) {
            for (int c = 0; c < nc; c++) {
                try {
                    ret.addElectrode(shank, c, r + row);
                } catch (ChannelHasBeenUsedException e) {
                }
            }
        }

        return ret;
    }

    public static ChannelMap npx24Stripe(int shank, double row) {
        var type = NpxProbeType.NP24;
        try {
            return npx24Stripe(shank, (int) (row / type.spacePerRow()));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().startsWith("row over range : ")) {
                throw new IllegalArgumentException("row over range : " + row + " um", e);
            } else {
                throw e;
            }
        }
    }

    public static ChannelMap npx24Stripe(int shank, int row) {
        var type = NpxProbeType.NP24;
        var ns = type.nShank();
        if (!(0 <= shank && shank < ns)) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var nc = type.nColumnPerShank();
        var nr = type.nChannel() / (nc * ns);
        if (!(0 <= row && row + nr < type.nRowPerShank())) {
            throw new IllegalArgumentException("row over range : " + row);
        }

        var ret = new ChannelMap(type);
        for (int s = 0; s < ns; s++) {
            for (int r = 0; r < nr; r++) {
                for (int c = 0; c < nc; c++) {
                    try {
                        ret.addElectrode(s, c, r + row);
                    } catch (ChannelHasBeenUsedException e) {
                    }
                }
            }
        }

        return ret;
    }

    public static ChannelMap npx24HalfDensity(int shank, double row) {
        var type = NpxProbeType.NP24;
        return npx24HalfDensity(shank, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24HalfDensity(int s1, int s2, double row) {
        var type = NpxProbeType.NP24;
        return npx24HalfDensity(s1, s2, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24HalfDensity(int shank, int row) {
        var type = NpxProbeType.NP24;
        if (!(0 <= shank && shank < type.nShank())) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 2) {
            addElectrode(ret, shank, 0, r + row);
            addElectrode(ret, shank, 1, r + row + 1);
        }
        for (int r = 192; r < 384; r += 2) {
            addElectrode(ret, shank, 1, r + row);
            addElectrode(ret, shank, 0, r + row + 1);
        }

        return ret;
    }

    public static ChannelMap npx24HalfDensity(int s1, int s2, int row) {
        var type = NpxProbeType.NP24;
        var ns = type.nShank();
        if (!(0 <= s1 && s1 < ns)) {
            throw new IllegalArgumentException("shank (s1) over range : " + s1);
        }
        if (!(0 <= s2 && s2 < ns)) {
            throw new IllegalArgumentException("shank (s2) over range : " + s2);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 2) {
            addElectrode(ret, s1, 0, r + row);
            addElectrode(ret, s1, 1, r + row + 1);
        }
        for (int r = 0; r < 192; r += 2) {
            addElectrode(ret, s2, 1, r + row);
            addElectrode(ret, s2, 0, r + row + 1);
        }

        return ret;
    }

    public static ChannelMap npx24QuarterDensity(double row) {
        var type = NpxProbeType.NP24;
        return npx24QuarterDensity((int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24QuarterDensity(int shank, double row) {
        var type = NpxProbeType.NP24;
        return npx24QuarterDensity(shank, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24QuarterDensity(int s1, int s2, double row) {
        var type = NpxProbeType.NP24;
        return npx24QuarterDensity(s1, s2, (int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24QuarterDensity(int row) {
        var type = NpxProbeType.NP24;
        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 4) {
            addElectrode(ret, 0, 0, r + row);
            addElectrode(ret, 0, 1, r + row + 2);
            addElectrode(ret, 1, 1, r + row);
            addElectrode(ret, 1, 0, r + row + 2);
            addElectrode(ret, 2, 0, r + row + 1);
            addElectrode(ret, 2, 1, r + row + 3);
            addElectrode(ret, 3, 1, r + row + 1);
            addElectrode(ret, 3, 0, r + row + 3);
        }

        return ret;
    }

    public static ChannelMap npx24QuarterDensity(int shank, int row) {
        var type = NpxProbeType.NP24;
        if (!(0 <= shank && shank < type.nShank())) {
            throw new IllegalArgumentException("shank over range : " + shank);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 4) {
            addElectrode(ret, shank, 0, r + row);
            addElectrode(ret, shank, 1, r + row + 2);
        }
        for (int r = 192; r < 384; r += 4) {
            addElectrode(ret, shank, 1, r + row);
            addElectrode(ret, shank, 0, r + row + 2);
        }
        for (int r = 384; r < 576; r += 4) {
            addElectrode(ret, shank, 0, r + row + 1);
            addElectrode(ret, shank, 1, r + row + 3);
        }
        for (int r = 576; r < 768; r += 4) {
            addElectrode(ret, shank, 1, r + row + 1);
            addElectrode(ret, shank, 0, r + row + 3);
        }

        return ret;
    }

    public static ChannelMap npx24QuarterDensity(int s1, int s2, int row) {
        var type = NpxProbeType.NP24;
        var ns = type.nShank();
        if (!(0 <= s1 && s1 < ns)) {
            throw new IllegalArgumentException("shank (s1) over range : " + s1);
        }
        if (!(0 <= s2 && s2 < ns)) {
            throw new IllegalArgumentException("shank (s2) over range : " + s2);
        }

        var nc = type.nColumnPerShank();
        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 4) {
            addElectrode(ret, s1, 0, r + row);
            addElectrode(ret, s1, 1, r + row + 2);
            addElectrode(ret, s2, 1, r + row + 1);
            addElectrode(ret, s2, 0, r + row + 3);
        }
        for (int r = 192; r < 384; r += 4) {
            addElectrode(ret, s1, 1, r + row);
            addElectrode(ret, s1, 0, r + row + 2);
            addElectrode(ret, s2, 0, r + row + 1);
            addElectrode(ret, s2, 1, r + row + 3);
        }

        return ret;
    }

    public static ChannelMap npx24OneEightDensity(double row) {
        var type = NpxProbeType.NP24;
        return npx24OneEightDensity((int) (row / type.spacePerRow()));
    }

    public static ChannelMap npx24OneEightDensity(int row) {
        var type = NpxProbeType.NP24;

        var nc = type.nColumnPerShank();

        var ret = new ChannelMap(type);
        for (int r = 0; r < 192; r += 8) {
            addElectrode(ret, 0, 0, r + row);
            addElectrode(ret, 1, 0, r + row + 1);
            addElectrode(ret, 2, 0, r + row + 2);
            addElectrode(ret, 3, 0, r + row + 3);
            addElectrode(ret, 0, 1, r + row + 5);
            addElectrode(ret, 1, 1, r + row + 6);
            addElectrode(ret, 2, 1, r + row + 7);
            addElectrode(ret, 3, 1, r + row + 8);
        }
        for (int r = 192; r < 384; r += 8) {
            addElectrode(ret, 0, 1, r + row);
            addElectrode(ret, 1, 1, r + row + 1);
            addElectrode(ret, 2, 1, r + row + 2);
            addElectrode(ret, 3, 1, r + row + 3);
            addElectrode(ret, 0, 0, r + row + 5);
            addElectrode(ret, 1, 0, r + row + 6);
            addElectrode(ret, 2, 0, r + row + 7);
            addElectrode(ret, 3, 0, r + row + 8);
        }

        return ret;
    }

    private static void addElectrode(ChannelMap ret, int shank, int column, int row) {
        try {
            ret.addElectrode(shank, column, row);
        } catch (ChannelHasBeenUsedException | IllegalArgumentException e) {
        }
    }

    public static String printProbe(ChannelMap chmap) {
        return printProbe(chmap, false, false);
    }

    public static String printProbe(ChannelMap chmap, boolean truncate) {
        return printProbe(chmap, truncate, false);
    }

    public static String printProbe(ChannelMap chmap, boolean truncate, boolean um) {
        var sb = new StringBuilder();
        try {
            printProbe(sb, chmap, truncate, um);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static void printProbe(PrintStream out, ChannelMap chmap) {
        printProbe(out, chmap, false, false);
    }

    public static void printProbe(PrintStream out, ChannelMap chmap, boolean truncate) {
        printProbe(out, chmap, truncate, false);
    }

    public static void printProbe(PrintStream out, ChannelMap chmap, boolean truncate, boolean um) {
        try {
            printProbe((Appendable) out, chmap, truncate, um);
        } catch (IOException e) {
        }
    }

    public static void printProbe(Appendable out, ChannelMap chmap) throws IOException {
        printProbe(out, chmap, false, false);
    }

    public static void printProbe(Appendable out, ChannelMap chmap, boolean truncate) throws IOException {
        printProbe(out, chmap, truncate, false);
    }

    public static void printProbe(Appendable out, ChannelMap chmap, boolean truncate, boolean um) throws IOException {
        var type = chmap.type();
        var nr = type.nRowPerShank() / 2;

        var sr = um ? type.spacePerRow() : 1;
        String[] lines = new String[nr]; // line number
        for (int i = 0; i < nr; i++) {
            lines[i] = Integer.toString(2 * i * sr);
        }
        int maxNrLength = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);

        var body = printProbeRaw(chmap);

        // body
        boolean checkTruncate = truncate;
        for (int r = nr - 1; r >= 0; r--) {
            var row = body.get(r);
            //noinspection AssignmentUsedAsCondition
            if (checkTruncate && (checkTruncate = isRowEmpty(row))) continue;

            for (int i = 0; i < maxNrLength - lines[r].length(); i++) out.append(' ');
            out.append(lines[r]);
            out.append(row);
            out.append('\n');
        }

        // tip
        for (int i = 0; i < maxNrLength; i++) out.append(' ');
        out.append(getProbeTip(type));
        out.append('\n');
    }


    public static String printProbe(List<ChannelMap> chmap) {
        return printProbe(chmap, false);
    }

    public static String printProbe(List<ChannelMap> chmap, boolean truncate) {
        var sb = new StringBuilder();
        try {
            printProbe(sb, chmap, truncate);
        } catch (IOException e) {
        }
        return sb.toString();
    }

    public static void printProbe(PrintStream out, List<ChannelMap> chmap) {
        printProbe(out, chmap, false);
    }

    public static void printProbe(PrintStream out, List<ChannelMap> chmap, boolean truncate) {
        try {
            printProbe((Appendable) out, chmap, truncate);
        } catch (IOException e) {
        }
    }

    public static void printProbe(Appendable out, List<ChannelMap> chmap) throws IOException {
        printProbe(out, chmap, false);
    }

    public static void printProbe(Appendable out, List<ChannelMap> chmap, boolean truncate) throws IOException {
        var nr = chmap.stream().mapToInt(it -> it.nRowPerShank() / 2).max().orElse(0);
        String[] rows = new String[nr];
        for (int i = 0; i < nr; i++) {
            rows[i] = Integer.toString(2 * i);
        }
        int maxNrLength = Arrays.stream(rows).mapToInt(String::length).max().orElse(0);

        var bodies = chmap.stream().map(ChannelMapUtil::printProbeRaw).toList();

        // body
        boolean checkTruncate = truncate;
        for (int r = nr - 1; r >= 0; r--) {
            int rr = r;
            if (checkTruncate) {
                checkTruncate = bodies.stream()
                  .map(it -> getRemappedRowContent(it, rr))
                  .allMatch(ChannelMapUtil::isRowEmpty);
                if (checkTruncate) continue;
            }

            for (int i = 0; i < maxNrLength - rows[r].length(); i++) out.append(' ');
            out.append(rows[r]);

            for (var body : bodies) {
                out.append(getRemappedRowContent(body, r));
                out.append("  ");
            }
            out.append('\n');
        }

        // tip
        for (int i = 0; i < maxNrLength; i++) out.append(' ');
        for (var m : chmap) {
            out.append(getProbeTip(m.type()));
            out.append("  ");
        }
        out.append('\n');
    }

    private static String getRemappedRowContent(List<String> content, int r) {
        if (r < content.size()) {
            return content.get(r);
        } else {
            return " ".repeat(content.get(0).length());
        }
    }

    private static final char[] PRINT_PROBE_UNICODE_SYMBOL = new char[]{
      ' ', '▖', '▘', '▌', '▗', '▄', '▚', '▙', '▝', '▞', '▀', '▛', '▐', '▟', '▜', '█',
    };
    private static final char[] PRINT_PROBE_UNICODE_SHAPE = new char[]{
      '▕', '▏',
    };

    private static List<String> printProbeRaw(ChannelMap chmap) {
        var type = chmap.type();
        var ns = type.nShank();
        var nr = type.nRowPerShank() / 2;
        var nc = type.nColumnPerShank() / 2;

        var arr = new int[ns * nr * nc];
        for (var e : chmap) {
            if (e != null) {
                var s = e.shank;
                var ci = e.column / 2;
                var cj = e.column % 2;
                var ri = e.row / 2;
                var rj = e.row % 2;
                var i = indexOf(ns, nr, nc, s, ri, ci);
                arr[i] = arr[i] | (cj == 0 ? 1 : 4) * (rj == 0 ? 1 : 2);
            }
        }

        var ret = new ArrayList<String>(nr);
        var tmp = new char[ns * (nc + 2)];

        for (int r = 0; r < nr; r++) {
            int k = 0;

            for (int s = 0; s < ns; s++) {
                tmp[k++] = PRINT_PROBE_UNICODE_SHAPE[0];
                for (int c = 0; c < nc; c++) {
                    tmp[k++] = PRINT_PROBE_UNICODE_SYMBOL[arr[indexOf(ns, nr, nc, s, r, c)]];
                }
                tmp[k++] = PRINT_PROBE_UNICODE_SHAPE[1];
            }
            ret.add(new String(tmp));
        }

        return ret;
    }

    private static String getProbeTip(NpxProbeType type) {
        var ns = type.nShank();
        var nc = type.nColumnPerShank() / 2;

        var tmp = new char[ns * (nc + 2)];

        // tip
        if (nc == 1) {
            int k = 0;
            for (int s = 0; s < ns; s++) {
                tmp[k++] = ' ';
                tmp[k++] = '╹';
                tmp[k++] = ' ';
            }
        } else if (nc == 2) {
            int k = 0;
            for (int s = 0; s < ns; s++) {
                tmp[k++] = ' ';
                tmp[k++] = '◥';
                tmp[k++] = '◤';
                tmp[k++] = ' ';
            }
        } else {
            int k = 0;
            for (int s = 0; s < ns; s++) {
                tmp[k++] = ' ';
                for (int i = 0; i < nc; i++) {
                    tmp[k++] = ' ';
                }
                tmp[k++] = ' ';
            }
        }
        return new String(tmp);
    }

    private static boolean isRowEmpty(String row) {
        for (int i = 1, len = row.length(); i < len; i++) {
            var c = row.charAt(i);
            if (!(c == PRINT_PROBE_UNICODE_SHAPE[0] || c == PRINT_PROBE_UNICODE_SHAPE[1] || c == PRINT_PROBE_UNICODE_SYMBOL[0])) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(int ns, int nr, int nc, int s, int r, int c) {
        // (R, S, C)
        return r * ns * nc + s * nc + c;
    }
}
