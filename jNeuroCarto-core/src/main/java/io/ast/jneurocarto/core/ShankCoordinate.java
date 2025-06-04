package io.ast.jneurocarto.core;

import java.util.function.IntFunction;

/**
 * The probe coordinate for each shank.
 */
@FunctionalInterface
public interface ShankCoordinate extends IntFunction<ProbeCoordinate> {

    ShankCoordinate ZERO = (shank) -> new ProbeCoordinate(shank, 0, 0);

    /**
     * @param shank shank index.
     * @return offset. Reuse {@link ProbeCoordinate} but changing fields' meaning to offset.
     */
    @Override
    ProbeCoordinate apply(int shank);

    /**
     * Offset between shanks.
     * <br/>
     * The default implement assume that the origin of shanks are collinear.
     *
     * @param fromShank shank index
     * @param toShank   shank index
     * @return a {@link ProbeCoordinate} used as an offset.
     */
    default ProbeCoordinate toShank(int fromShank, int toShank) {
        return apply(toShank - fromShank);
    }

    /**
     * Offset the {@link ProbeCoordinate} to the new coordinate on the {@code shank}.
     * <br/>
     * The default implement assume that the origin of shanks are collinear.
     *
     * @param coor  from coordinate
     * @param shank shank index
     * @return a new coordinate on {@code shank}.
     */
    default ProbeCoordinate toShank(ProbeCoordinate coor, int shank) {
        if (coor.s() == shank) return coor;
        var offset = toShank(coor.s(), shank);
        return new ProbeCoordinate(shank, coor.x() + offset.x(), coor.y() + offset.y(), coor.z() + offset.z());
    }

    /**
     * For the probe that all shanks' origin are collinear along an x-axis.
     *
     * @param dx offset x (um) per shank.
     * @return a transformator.
     */
    static ShankCoordinate linear(double dx) {
        return (shank) -> new ProbeCoordinate(shank, shank * dx, 0, 0);
    }

    /**
     * For the probe that all shanks' origin are collinear along an x-z vector.
     *
     * @param dx offset x (um) per shank.
     * @param dz offset z (um) per shank.
     * @return a transformator.
     */
    static ShankCoordinate linear(double dx, double dz) {
        return (shank) -> new ProbeCoordinate(shank, shank * dx, 0, shank * dz);
    }

    /**
     * For the probe that all shanks' origin are collinear along a vector.
     *
     * @param dx offset x (um) per shank.
     * @param dy offset y (um) per shank.
     * @param dz offset z (um) per shank.
     * @return a transformator.
     */
    static ShankCoordinate linear(double dx, double dy, double dz) {
        return (shank) -> new ProbeCoordinate(shank, shank * dx, shank * dy, shank * dz);
    }
}
