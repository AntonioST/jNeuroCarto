package io.ast.jneurocarto.atlas;

import java.util.HashSet;
import java.util.Map;

public class AnatomicalSpace {

    public enum Axes {
        sagittal('p', 'a'), vertical('s', 'i'), frontal('l', 'r');

        public final char label1;
        public final char label2;

        Axes(char lim1, char lim2) {
            this.label1 = lim1;
            this.label2 = lim2;
        }

        static Axes valueOf(int i) {
            return values()[Math.abs(i) - 1];
        }

        static char labelOf(int i) {
            var ax = values()[Math.abs(i) - 1];
            return i > 0 ? ax.label1 : ax.label2;
        }
    }

    public enum Labels {
        posterior('p'), anterior('a'), superior('s'), inferior('i'), left('l'), right('r');

        private final char code;

        Labels(char code) {
            this.code = code;
        }

        static Labels valueOf(char c) {
            return switch (Character.toLowerCase(c)) {
                case 'p' -> posterior;
                case 'a' -> anterior;
                case 's' -> superior;
                case 'o' -> inferior;
                case 'l' -> left;
                case 'r' -> right;
                default -> throw new IllegalArgumentException();
            };
        }
    }

    public static final Map<String, String> MAP_PLANES_FROM_AXES = Map.of(
      "sagittal", "pasi",
      "frontal", "silr",
      "horizontal", "palr"
    );

    private final int[] axes;
    private final int[] shape;
    private final double[] resolution;
    private final int[] offset;

    public AnatomicalSpace(String origin, int[] shape, double[] resolution, int[] offset) {
        this.axes = getAxesDesp(origin);
        this.shape = shape;
        this.resolution = resolution;
        this.offset = offset;
    }

    public AnatomicalSpace(String[] origin, int[] shape, double[] resolution, int[] offset) {
        this.axes = getAxesDesp(origin);
        this.shape = shape;
        this.resolution = resolution;
        this.offset = offset;
    }

    private static int[] getAxesDesp(String origin) {
        var ret = new String[origin.length()];
        for (int i = 0, length = ret.length; i < length; i++) {
            ret[i] = origin.substring(i, i + 1);
        }
        return getAxesDesp(ret);
    }

    private static int[] getAxesDesp(String[] origin) {
        if (origin.length != 3) throw new IllegalArgumentException();

        var ret = new int[3];
        var dir = new HashSet<Axes>();

        for (int i = 0; i < 3; i++) {
            var lim = origin[i].toLowerCase().charAt(0);

            for (var axes : Axes.values()) {
                if (lim == axes.label1) {
                    dir.add(axes);
                    ret[i] = axes.ordinal() + 1;
                } else if (lim == axes.label2) {
                    dir.add(axes);
                    ret[i] = -(axes.ordinal() + 1);
                }
            }
        }

        if (dir.size() != 3) throw new IllegalArgumentException();

        return ret;
    }

    public Axes[] axesOrder() {
        var order = new Axes[axes.length];
        for (int i = 0, length = order.length; i < length; i++) {
            order[i] = Axes.valueOf(axes[i]);
        }
        return order;
    }

    public String origin() {
        var ret = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            ret.append(Axes.labelOf(axes[i]));
        }
        return ret.toString();
    }

    public int getAxisIndexOf(String axes) {
        return Axes.valueOf(axes).ordinal();
    }

    public int getAxisIndexOf(Axes axes) {
        return axes.ordinal();
    }

    public Labels[][] axisLabels() {
        var ax00 = -axes[1]; //  Flip for images
        var ax01 = axes[2];
        var ax10 = -axes[0];
        var ax11 = axes[2];
        var ax20 = -axes[0];
        var ax21 = axes[1];
        return new Labels[][]{
          {Labels.valueOf(Axes.labelOf(ax00)), Labels.valueOf(Axes.labelOf(ax01))},
          {Labels.valueOf(Axes.labelOf(ax10)), Labels.valueOf(Axes.labelOf(ax11))},
          {Labels.valueOf(Axes.labelOf(ax20)), Labels.valueOf(Axes.labelOf(ax21))},
        };
    }
}
