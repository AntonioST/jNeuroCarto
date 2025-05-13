package io.ast.jneurocarto.core;

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

    private static final ShankCoordinateTransformator SHANK_COOR_IDENTIFY = ProbeCoordinate::new;

    /**
     * {@snippet lang = "TEXT":
     *  [dap/dx, dap/dy, dap/dz;
     *   ddv/dx, ddv/dy, ddv/dz;
     *   dml/dx, dml/dy, dml/dz]
     *}
     */
    private final double[] transform;

    /**
     * {@snippet lang = "TEXT":
     *  [dx/dap, dx/ddv, dx/dml;
     *   dy/dap, dy/ddv, dy/dml;
     *   dz/dap, dz/ddv, dz/dml]
     *}
     */
    private final double[] invTransform;

    private ShankCoordinateTransformator shankTransform = SHANK_COOR_IDENTIFY;

    public ProbeCoordinateTransformator(double[] transform) {
        if (transform.length != 9) throw new IllegalArgumentException();
        this.transform = transform.clone();
        invTransform = inverse(transform);
    }

    private static double[] inverse(double[] transform) {
        // by GPT
        double a = transform[0], b = transform[1], c = transform[2];
        double d = transform[3], e = transform[4], f = transform[5];
        double g = transform[6], h = transform[7], i = transform[8];

        double det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);

        if (Math.abs(det) < 1e-10) {
            throw new ArithmeticException("Matrix is singular and cannot be inverted.");
        }

        double invDet = 1.0 / det;

        double[] inv = new double[9];

        inv[0] = +(e * i - f * h) * invDet;
        inv[1] = -(b * i - c * h) * invDet;
        inv[2] = +(b * f - c * e) * invDet;

        inv[3] = -(d * i - f * g) * invDet;
        inv[4] = +(a * i - c * g) * invDet;
        inv[5] = -(a * f - c * d) * invDet;

        inv[6] = +(d * h - e * g) * invDet;
        inv[7] = -(a * h - b * g) * invDet;
        inv[8] = +(a * e - b * d) * invDet;

        return inv;
    }

    public ShankCoordinateTransformator getShankTransform() {
        return shankTransform;
    }

    public void setShankTransform(ShankCoordinateTransformator transform) {
        this.shankTransform = transform;
    }

    public ImplantCoordinate toShank(ImplantCoordinate coor, int shank) {
        var offset = shankTransform.offset(shank - coor.s());
        var x = offset.x();
        var y = offset.y();
        var z = offset.z();
        var m = transform;
        var ap = m[0] * x + m[1] * y + m[2] * z;
        var dv = m[3] * x + m[4] * y + m[5] * z;
        var ml = m[6] * x + m[7] * y + m[8] * z;
        return coor.offset(ap, dv, ml);
    }

    public ProbeCoordinate offset(ProbeCoordinate coor, double ap, double dv, double ml) {
        var m = invTransform;
        var x = m[0] * ap + m[1] * dv + m[2] * ml;
        var y = m[3] * ap + m[4] * dv + m[5] * ml;
        var z = m[6] * ap + m[7] * dv + m[8] * ml;
        return new ProbeCoordinate(coor.s(), coor.x() + x, coor.y() + y, coor.z() + z);
    }

    public ProbeCoordinate offset(ProbeCoordinate coor, Coordinate offset) {
        return offset(coor, offset.ap(), offset.dv(), offset.ml());
    }

    public ProbeCoordinate toProbeCoordinate(ImplantCoordinate coor, double depth) {
        var c = coor.toCoordinate(depth); // FIXME it is not correct.
        var ap = c.ap();
        var dv = c.dv();
        var ml = c.ml();
        var m = invTransform;
        var x = m[0] * ap + m[1] * dv + m[2] * ml;
        var y = m[3] * ap + m[4] * dv + m[5] * ml;
        return new ProbeCoordinate(coor.s(), x, y);
    }

    public ProbeCoordinate toProbeCoordinate(ImplantCoordinate coor, int shank, double depth) {
        return toProbeCoordinate(toShank(coor, shank), depth);
    }


}
