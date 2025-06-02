package io.ast.jneurocarto.core;

import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

public class ProbeCoordinateTransformator {

    @FunctionalInterface
    public interface ShankCoordinateTransformator {
        /**
         * @param shank shank offset.
         * @return offset. Reuse {@link ProbeCoordinate} but changing fields' meaning to offset.
         */
        ProbeCoordinate offset(int shank);

        default ProbeCoordinate toShank(ProbeCoordinate coor, int shank) {
            var offset = offset(shank - coor.s());
            return new ProbeCoordinate(shank, coor.x() + offset.x(), coor.y() + offset.y());
        }

        /**
         * For the probe that all shanks are lied on the same plane.
         *
         * @param dx offset x (um) per shank.
         * @return a transformator.
         */
        static ShankCoordinateTransformator linear(double dx) {
            return (shank) -> new ProbeCoordinate(shank, shank * dx, 0, 0);
        }

        /**
         * For the probe that all shanks are lied on the same plane.
         *
         * @param dx offset x (um) per shank.
         * @param dz offset z (um) per shank.
         * @return a transformator.
         */
        static ShankCoordinateTransformator linear(double dx, double dz) {
            return (shank) -> new ProbeCoordinate(shank, shank * dx, 0, shank * dz);
        }

        /**
         * For the probe that all shanks are lied on the same plane.
         *
         * @param dx offset x (um) per shank.
         * @param dy offset y (um) per shank.
         * @param dz offset z (um) per shank.
         * @return a transformator.
         */
        static ShankCoordinateTransformator linear(double dx, double dy, double dz) {
            return (shank) -> new ProbeCoordinate(shank, shank * dx, shank * dy, shank * dz);
        }
    }

    private static final Point3D AXIS_AP = new Point3D(1, 0, 0);
    private static final Point3D AXIS_DV = new Point3D(0, 1, 0);
    private static final Point3D AXIS_ML = new Point3D(0, 0, 1);

    private static final ShankCoordinateTransformator SHANK_COOR_IDENTIFY = ProbeCoordinate::new;

    /**
     * transform from probe coordinate to altas coordinate.
     */
    private final Affine transform;

    private ShankCoordinateTransformator shankTransform = SHANK_COOR_IDENTIFY;

    public ProbeCoordinateTransformator(Affine transform) {
        this.transform = transform;
    }

    public ShankCoordinateTransformator getShankTransform() {
        return shankTransform;
    }

    public void setShankTransform(ShankCoordinateTransformator transform) {
        this.shankTransform = transform;
    }

    /*==================================*
     * ImplantCoordinate transformation *
     *==================================*/

    public ImplantCoordinate toShank(ImplantCoordinate coor, int shank) {
        var o = shankTransform.offset(shank - coor.s());
        var c = toGlobalCoordinate(o);
        return coor.offset(c);
    }

    /*================================*
     * Coordinate <-> ProbeCoordinate *
     *================================*/

    private ProbeCoordinate toProbeCoordinate(Coordinate offset) {
        return toProbeCoordinate(0, offset);
    }

    private ProbeCoordinate toProbeCoordinate(int shank, Point3D coor) {
        Affine m;
        try {
            m = transform.createInverse();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }
        return new ProbeCoordinate(shank, m.transform(coor));
    }

    private ProbeCoordinate toProbeCoordinate(int shank, Coordinate coor) {
        Affine m;
        try {
            m = transform.createInverse();
        } catch (NonInvertibleTransformException e) {
            throw new RuntimeException(e);
        }
        return new ProbeCoordinate(shank, m.transform(coor.ap(), coor.dv(), coor.ml()));
    }

    private Coordinate toGlobalCoordinate(ProbeCoordinate coor) {
        return new Coordinate(transform.transform(coor.x(), coor.y(), coor.z()));
    }

    /*=======================================*
     * ImplantCoordinate --> ProbeCoordinate *
     *=======================================*/

    /**
     * get probe coordinate at insertion position.
     *
     * @param coor
     * @return
     */
    public ProbeCoordinate toProbeInsertCoordinate(ImplantCoordinate coor) {
        var c = coor.insertCoordinate();
        return toProbeCoordinate(coor.s(), c);
    }

    /**
     * get probe coordinate at {@code shank}'s insertion position.
     *
     * @param coor
     * @param shank
     * @return
     */
    public ProbeCoordinate toProbeInsertCoordinate(ImplantCoordinate coor, int shank) {
        return toProbeInsertCoordinate(toShank(coor, shank));
    }

    /**
     * get probe coordinate at probe tip position.
     *
     * @param coor
     * @return
     */
    public ProbeCoordinate toProbeTipCoordinate(ImplantCoordinate coor) {
        return toProbeCoordinate(coor, coor.depth());
    }

    /**
     * get probe coordinate at probe's shank-th tip position.
     *
     * @param coor
     * @param shank
     * @return
     */
    public ProbeCoordinate toProbeTipCoordinate(ImplantCoordinate coor, int shank) {
        return toProbeCoordinate(toShank(coor, shank), coor.depth());
    }

    /**
     * @param coor
     * @param depth insert distance in um.
     * @return
     */
    public ProbeCoordinate toProbeCoordinate(ImplantCoordinate coor, double depth) {
        var z = Point3D.ZERO;
        var m = new Affine(transform);
        m.appendRotation(coor.rap(), z, AXIS_AP);
        m.appendRotation(coor.rdv(), z, AXIS_DV);
        m.appendRotation(coor.rml(), z, AXIS_ML);
        var o = m.transform(0, depth, 0).add(coor.ap(), coor.dv(), coor.ml());
        return toProbeCoordinate(coor.s(), o);
    }

    /**
     * @param coor
     * @param shank
     * @param depth insert distance in um.
     * @return
     */
    public ProbeCoordinate toProbeCoordinate(ImplantCoordinate coor, int shank, double depth) {
        return toProbeCoordinate(toShank(coor, shank), depth);
    }

    /*======================*
     * Probe transformation *
     *======================*/

    public ProbeCoordinate offset(ProbeCoordinate coor, double ap, double dv, double ml) {
        return offset(coor, new Coordinate(ap, dv, ml));
    }

    /**
     * offset the probe coordinate.
     *
     * @param coor
     * @param offset the offset under global coordinate.
     * @return
     */
    public ProbeCoordinate offset(ProbeCoordinate coor, Coordinate offset) {
        return coor.offset(toProbeCoordinate(offset));
    }


}
