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

    private ProbeCoordinate toProbeCoordinate(int shank, Coordinate coor) {
        var ap = coor.ap();
        var dv = coor.dv();
        var ml = coor.ml();
        var m = invTransform;
        var x = m[0] * ap + m[1] * dv + m[2] * ml;
        var y = m[3] * ap + m[4] * dv + m[5] * ml;
        return new ProbeCoordinate(shank, x, y);
    }

    private Coordinate toGlobalCoordinate(ProbeCoordinate coor) {
        var x = coor.x();
        var y = coor.y();
        var z = coor.z();
        var m = transform;
        var ap = m[0] * x + m[1] * y + m[2] * z;
        var dv = m[3] * x + m[4] * y + m[5] * z;
        var ml = m[6] * x + m[7] * y + m[8] * z;
        return new Coordinate(ap, dv, ml);
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
        var r = newRotateMatrix(coor.rap(), coor.rdv(), coor.rml());
        var o = rotate(new Coordinate(0, depth, 0), r);
        var c = coor.insertCoordinate().offset(o);
        return toProbeCoordinate(coor.s(), c);
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

    /**
     * offset the probe coordinate with taking rotation into account.
     *
     * @param implant
     * @param coor
     * @param offset  the offset under global coordinate without considering rotation.
     * @return
     */
    public ProbeCoordinate offset(ImplantCoordinate implant, ProbeCoordinate coor, Coordinate offset) {
        var r = newRotateMatrix(implant.rap(), implant.rdv(), implant.rml());
        var o = rotate(offset, r);
        return coor.offset(toProbeCoordinate(o));
    }

    /*====================*
     * rotation utilities *
     *====================*/

    /**
     * {@snippet lang = "TEXT":
     *   [dap/dap, dap/ddv, dap/dml, dap/1;
     *    ddv/dap, ddv/ddv, ddv/dml, ddv/1;
     *    dml/dap, dml/ddv, dml/dml, ddv/1;
     *      1/dap,   1/dap,   1/dml,   1/1]
     *}
     *
     * @param rap
     * @param rdv
     * @param rml
     * @return
     */
    private static double[] newRotateMatrix(double rap, double rdv, double rml) {

        var i00 = 0;
        var i01 = 1;
        var i02 = 2;
//        var i03 = 3;
        var i10 = 4;
        var i11 = 5;
        var i12 = 6;
//        var i13 = 7;
        var i20 = 8;
        var i21 = 9;
        var i22 = 10;
//        var i23 = 11;
//        var i30 = 12;
//        var i31 = 13;
//        var i32 = 14;
        var i33 = 15;

        var ret = new double[9];
        ret[i00] = ret[i11] = ret[i22] = ret[i33] = 1;

        var tmp = ret.clone();

        /*
        1,   0,    0, 0, // ap
        0, cos, -sin, 0, // dv
        0, sin,  cos, 0, // ml
        0,   0,    0, 1  // 1
         */
        var cos = Math.cos(rap);
        var sin = Math.sin(rap);
        tmp[i11] = cos;
        tmp[i12] = -sin;
        tmp[i21] = sin;
        tmp[i22] = cos;
        product(ret, tmp, ret);

        /*
         cos, 0, sin, 0, // ap
           0, 1,   0, 0, // dv
        -sin, 0, cos, 0, // ml
           0, 0,   0, 1  // 1
         */
        cos = Math.cos(rdv);
        sin = Math.sin(rdv);
        tmp[i11] = 1;
        tmp[i12] = 0;
        tmp[i21] = 0;
//        tmp[i22] = 1;
        tmp[i00] = cos;
        tmp[i02] = sin;
        tmp[i20] = -sin;
        tmp[i22] = cos;
        product(ret, tmp, ret);

        /*
        cos, -sin, 0, 0, // ap
        sin,  cos, 0, 0, // dv
          0,    0, 1, 0, // ml
          0,    0, 0, 1  // 1
         */
        cos = Math.cos(rml);
        sin = Math.sin(rml);
//        tmp[i00] = 1;
        tmp[i02] = 0;
        tmp[i20] = 0;
        tmp[i22] = 1;
        tmp[i00] = cos;
        tmp[i01] = -sin;
        tmp[i10] = sin;
        tmp[i11] = cos;
        product(ret, tmp, ret);

        return ret;
    }

    private static double[] product(double[] a, double[] b, double[] c) {
        assert b != c;

        var A = 10;
        var B = 11;
        var C = 12;
        var D = 13;
        var E = 14;
        var F = 15;

        var t0 = a[0] * b[0] + a[1] * b[4] + a[2] * b[8] + a[3] * b[C];
        var t1 = a[0] * b[1] + a[1] * b[5] + a[2] * b[9] + a[3] * b[D];
        var t2 = a[0] * b[2] + a[1] * b[6] + a[2] * b[A] + a[3] * b[E];
        var t3 = a[0] * b[3] + a[1] * b[7] + a[2] * b[B] + a[3] * b[F];
        c[0] = t0;
        c[1] = t1;
        c[2] = t2;
        c[3] = t3;

        t0 = a[4] * b[0] + a[5] * b[4] + a[6] * b[8] + a[7] * b[C];
        t1 = a[4] * b[1] + a[5] * b[5] + a[6] * b[9] + a[7] * b[D];
        t2 = a[4] * b[2] + a[5] * b[6] + a[6] * b[A] + a[7] * b[E];
        t3 = a[4] * b[3] + a[5] * b[7] + a[6] * b[B] + a[7] * b[F];
        c[4] = t0;
        c[5] = t1;
        c[6] = t2;
        c[7] = t3;

        t0 = a[8] * b[0] + a[9] * b[4] + a[A] * b[8] + a[B] * b[C];
        t1 = a[8] * b[1] + a[9] * b[5] + a[A] * b[9] + a[B] * b[D];
        t2 = a[8] * b[2] + a[9] * b[6] + a[A] * b[A] + a[B] * b[E];
        t3 = a[8] * b[3] + a[9] * b[7] + a[A] * b[B] + a[B] * b[F];
        c[8] = t0;
        c[9] = t1;
        c[A] = t2;
        c[B] = t3;

        t0 = a[C] * b[0] + a[D] * b[4] + a[E] * b[8] + a[F] * b[C];
        t1 = a[C] * b[1] + a[D] * b[5] + a[E] * b[9] + a[F] * b[D];
        t2 = a[C] * b[2] + a[D] * b[6] + a[E] * b[A] + a[F] * b[E];
        t3 = a[C] * b[3] + a[D] * b[7] + a[E] * b[B] + a[F] * b[F];
        c[C] = t0;
        c[D] = t1;
        c[E] = t2;
        c[F] = t3;

        return c;
    }

    private static Coordinate rotate(Coordinate coor, double[] r) {
        var ap = coor.ap();
        var dv = coor.dv();
        var ml = coor.ml();
        var nap = r[0] * ap + r[1] * dv + r[2] * ml + r[3] * 1;
        var ndv = r[4] * ap + r[5] * dv + r[6] * ml + r[7] * 1;
        var nml = r[8] * ap + r[9] * dv + r[10] * ml + r[11] * 1;
        return new Coordinate(nap, ndv, nml);
    }


}
