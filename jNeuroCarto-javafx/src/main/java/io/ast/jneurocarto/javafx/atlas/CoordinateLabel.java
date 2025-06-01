package io.ast.jneurocarto.javafx.atlas;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.ImageSliceStack;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import io.ast.jneurocarto.core.Coordinate;

@NullMarked
public record CoordinateLabel(String text, @Nullable LabelPosition position, String color) {

    public enum LabelPositionKind {
        atlas("Atlas Coordinate"),
        reference("Atlas Referenced Coordinate"),
        slice("Brain Slice"),
        chart("Chart"),
        probe("Probe");

        public final String kind;

        LabelPositionKind(String kind) {
            this.kind = kind;
        }
    }

    public sealed interface LabelPosition {
        LabelPositionKind kind();
    }


    public record AtlasPosition(@Nullable Coordinate coordinate) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.atlas;
        }
    }

    public record AtlasRefPosition(String reference, @Nullable Coordinate coordinate) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.reference;
        }
    }

    public record SlicePosition(ImageSliceStack.Projection projection, @Nullable SliceCoordinate coordinate) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.slice;
        }
    }

    public record ChartPosition(double x, double y) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.chart;
        }
    }

    public record ProbePosition(double x, double y) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.probe;
        }
    }

    public static List<CoordinateLabel> getAll(AtlasLabelViewState state) {
        return state.labels.stream().map(CoordinateLabel::of).toList();
    }

    public static CoordinateLabel get(AtlasLabelViewState state, int index) {
        return of(state.labels.get(index));
    }

    public static AtlasLabelViewState newState(List<CoordinateLabel> labels) {
        var ret = new AtlasLabelViewState();
        ret.labels.addAll(labels.stream().map(CoordinateLabel::of).toList());
        return ret;
    }

    public static CoordinateLabel of(AtlasLabelViewState.Label label) {
        var text = label.text;
        var color = label.color;
        var array = label.position;
        var pos = switch (label.type) {
            case "atlas" -> {
                if (array == null) {
                    yield new AtlasPosition(null);
                } else {
                    yield new AtlasPosition(new Coordinate(array[0], array[1], array[2]));
                }
            }
            case "reference" -> {
                if (array == null) {
                    yield new AtlasRefPosition(label.reference, null);
                } else {
                    yield new AtlasRefPosition(label.reference, new Coordinate(array[0], array[1], array[2]));
                }
            }
            case "slice" -> {
                var projection = ImageSliceStack.Projection.valueOf(label.reference);
                if (array == null) {
                    yield new SlicePosition(projection, null);
                } else {
                    yield new SlicePosition(projection, new SliceCoordinate(array[0], array[1], array[2]));
                }
            }
            case "chart" -> {
                if (array == null) {
                    yield new ChartPosition(0, 0);
                } else {
                    yield new ChartPosition(array[0], array[1]);
                }
            }
            case "probe" -> {
                if (array == null) {
                    yield new ProbePosition(0, 0);
                } else {
                    yield new ProbePosition(array[0], array[1]);
                }
            }
            default -> null;
        };

        return new CoordinateLabel(text, pos, color);
    }

    public static AtlasLabelViewState.Label of(CoordinateLabel label) {
        var ret = new AtlasLabelViewState.Label();
        ret.text = label.text;
        ret.color = label.color;
        switch (label.position) {
        case null -> {
            ret.position = null;
            ret.type = "";
        }
        case AtlasPosition(var coor) when coor == null -> {
            ret.position = null;
            ret.type = "atlas";
        }
        case AtlasPosition(var coor) -> {
            ret.position = new double[]{coor.ap(), coor.dv(), coor.ml()};
            ret.type = "atlas";
        }
        case AtlasRefPosition(var reference, var coor) when coor == null -> {
            ret.position = null;
            ret.reference = reference;
            ret.type = "reference";
        }
        case AtlasRefPosition(var reference, var coor) -> {
            ret.position = new double[]{coor.ap(), coor.dv(), coor.ml()};
            ret.reference = reference;
            ret.type = "reference";
        }
        case SlicePosition(var projection, var coor) when coor == null -> {
            ret.position = null;
            ret.reference = projection.name();
            ret.type = "slice";
        }
        case SlicePosition(var projection, var coor) -> {
            ret.position = new double[]{coor.p(), coor.x(), coor.y()};
            ret.reference = projection.name();
            ret.type = "slice";
        }
        case ChartPosition(var x, var y) -> {
            ret.position = new double[]{x, y};
            ret.type = "chart";
        }
        case ProbePosition(var x, var y) -> {
            ret.position = new double[]{x, y};
            ret.type = "probe";
        }
        }
        return ret;
    }

}
