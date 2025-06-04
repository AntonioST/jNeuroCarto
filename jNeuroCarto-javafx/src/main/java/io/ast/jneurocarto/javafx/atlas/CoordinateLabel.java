package io.ast.jneurocarto.javafx.atlas;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.ImageSliceStack;
import io.ast.jneurocarto.atlas.SliceCoordinate;
import io.ast.jneurocarto.core.Coordinate;
import io.ast.jneurocarto.core.ProbeCoordinate;

@NullMarked
public record CoordinateLabel(String text, LabelPosition position, String color) {

    public enum LabelPositionKind {
        atlas("Atlas Coordinate"),
        reference("Atlas Referenced Coordinate"),
        slice("Brain Slice"),
        probe("Probe");

        public final String kind;

        LabelPositionKind(String kind) {
            this.kind = kind;
        }
    }

    public sealed interface LabelPosition {
        LabelPositionKind kind();
    }

    /**
     * @param coordinate anatomical coordinate
     * @param reference  reference name
     */
    public record AtlasPosition(Coordinate coordinate, @Nullable String reference) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.atlas;
        }
    }

    public record SlicePosition(ImageSliceStack.Projection projection,
                                SliceCoordinate coordinate) implements LabelPosition {
        @Override
        public LabelPositionKind kind() {
            return LabelPositionKind.slice;
        }
    }

    public record ProbePosition(ProbeCoordinate coordinate) implements LabelPosition {
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
                Coordinate coor;
                if (array == null) {
                    coor = new Coordinate(0, 0, 0);
                } else {
                    coor = new Coordinate(array[0], array[1], array[2]);
                }
                yield new AtlasPosition(coor, label.reference);
            }
            case "slice" -> {
                var projection = ImageSliceStack.Projection.valueOf(label.projection);
                SliceCoordinate coor;
                if (array == null) {
                    coor = new SliceCoordinate(0, 0, 0);
                } else {
                    coor = new SliceCoordinate(array[0], array[1], array[2]);
                }

                yield new SlicePosition(projection, coor);
            }
            case "probe" -> {
                if (array == null) {
                    yield new ProbePosition(new ProbeCoordinate(0, 0, 0));
                } else {
                    yield new ProbePosition(new ProbeCoordinate((int) array[2], array[0], array[1]));
                }
            }
            default -> new ProbePosition(new ProbeCoordinate(0, 0, 0));
        };

        return new CoordinateLabel(text, pos, color);
    }

    public static AtlasLabelViewState.Label of(CoordinateLabel label) {
        var ret = new AtlasLabelViewState.Label();
        ret.text = label.text;
        ret.color = label.color;
        switch (label.position) {
        case AtlasPosition(var coor, var reference) -> {
            ret.position = new double[]{coor.ap(), coor.dv(), coor.ml()};
            ret.type = "atlas";
            ret.reference = reference;
        }
        case SlicePosition(var projection, var coor) -> {
            ret.position = new double[]{coor.p(), coor.x(), coor.y()};
            ret.projection = projection.name();
            ret.type = "slice";
        }
        case ProbePosition(var coor) -> {
            ret.position = new double[]{coor.x(), coor.y(), coor.s()};
            ret.type = "probe";
        }
        }
        return ret;
    }

    public CoordinateLabel withPosition(LabelPosition position) {
        switch (this.position) {
        case AtlasPosition(_, var ref) -> {
            if (position instanceof AtlasPosition(_, var other) && Objects.equals(ref, other)) {
                return new CoordinateLabel(text, position, color);
            }
        }
        case SlicePosition(var proj, _) -> {
            if (position instanceof SlicePosition(var other, _) && proj == other) {
                return new CoordinateLabel(text, position, color);
            }
        }
        case ProbePosition _ -> {
            if (position instanceof ProbePosition) {
                return new CoordinateLabel(text, position, color);
            }
        }
        }
        throw new IllegalArgumentException("new position do not have same the kind position");
    }

}
