package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A neural probe implant point, using the coordinate system in the anatomical space.
 *
 * @param ap        ap insertion position in um
 * @param dv        dv insertion position in um
 * @param ml        ml insertion position in um
 * @param s         the shank of the insertion position.
 * @param rap       rotation along ap-axis in degree
 * @param rdv       rotation along dv-axis in degree
 * @param rml       rotation along ml-axis in degree
 * @param depth     insert depth in um
 * @param reference insertion position reference.
 */
@NullMarked
public record ImplantCoordinate(
    double ap,
    double dv,
    double ml,
    int s,
    double rap,
    double rdv,
    double rml,
    double depth,
    @Nullable String reference
) {

    public ImplantCoordinate {
        if (s < 0) throw new IllegalArgumentException("negative shank : " + s);
        rap %= 360;
        rdv %= 360;
        rml %= 360;
    }

    /**
     * Create an implant coordinate with given insertion coordinate.
     *
     * @param ap ap position in um.
     * @param ml ml position in um.
     */
    public ImplantCoordinate(double ap, double ml) {
        this(ap, 0, ml, 0, 0, 0, 0, 0, null);
    }

    /**
     * Create an implant coordinate with given insertion coordinate.
     *
     * @param ap        ap position reference to {@code reference} in um.
     * @param ml        ml position reference to {@code reference} in um.
     * @param reference insertion position reference.
     */
    public ImplantCoordinate(double ap, double ml, String reference) {
        this(ap, 0, ml, 0, 0, 0, 0, 0, reference);
    }

    /**
     * Create an implant coordinate with given insertion coordinate.
     *
     * @param ap    ap position in um.
     * @param ml    ml position in um.
     * @param depth insert depth
     */
    public ImplantCoordinate(double ap, double ml, double depth) {
        this(ap, 0, ml, 0, 0, 0, 0, depth, null);
    }

    /**
     * Create an implant coordinate with given insertion coordinate.
     *
     * @param ap        ap position reference to {@code reference} in um.
     * @param ml        ml position reference to {@code reference} in um.
     * @param depth     insert depth
     * @param reference insertion position reference.
     */
    public ImplantCoordinate(double ap, double ml, double depth, String reference) {
        this(ap, 0, ml, 0, 0, 0, 0, depth, reference);
    }

    private void checkSourceDomain(ProbeTransform<Coordinate, ?> transform) {
        if (reference == null) {
            if (!(transform.sourceDomain() instanceof ProbeTransform.Anatomical)) {
                throw new IllegalArgumentException("source domain is not a global anatomical space");
            }
        } else {
            if (!(transform.sourceDomain() instanceof ProbeTransform.ReferencedAnatomical(var reference, _, _))) {
                throw new IllegalArgumentException("source domain is not a referenced anatomical space");
            } else if (!this.reference.equals(reference)) {
                throw new IllegalArgumentException("source referenced domain does not match to the " + this.reference + " reference");
            }
        }
    }

    private void checkTargetDomain(ProbeTransform<?, Coordinate> transform) {
        if (reference == null) {
            if (!(transform.targetDomain() instanceof ProbeTransform.Anatomical)) {
                throw new IllegalArgumentException("target domain is not a global anatomical space");
            }
        } else {
            if (!(transform.targetDomain() instanceof ProbeTransform.ReferencedAnatomical(var ref, _, _))) {
                throw new IllegalArgumentException("target domain is not a referenced anatomical space");
            } else if (!reference.equals(ref)) {
                throw new IllegalArgumentException("target referenced domain does not match to the " + reference + " reference");
            }
        }
    }

    /**
     * Transform the implant coordinate between different reference point.
     *
     * @param transform a {@link ProbeTransform}, which its source domain should match to {@link #reference}.
     * @return a new implant coordinate with the reference match to {@code transform}'s target domain.
     * @throws IllegalArgumentException The source or target domain of {@code transform} does not an anatomical space, or
     *                                  The source domain of {@code transform} does not match to the {@link #reference}.
     */
    public ImplantCoordinate changeReference(ProbeTransform<Coordinate, Coordinate> transform) {
        checkSourceDomain(transform);

        String target;
        boolean sfap;
        boolean tfap;

        switch (transform.sourceDomain()) {
        case ProbeTransform.Anatomical _ -> sfap = false;
        case ProbeTransform.ReferencedAnatomical(_, _, var fap) -> sfap = fap;
        default -> throw new RuntimeException("unknown target domain");
        }

        switch (transform.targetDomain()) {
        case ProbeTransform.Anatomical _ -> {
            target = null;
            tfap = false;
        }
        case ProbeTransform.ReferencedAnatomical(var ref, _, var fap) -> {
            target = ref;
            tfap = fap;
        }
        default -> throw new IllegalArgumentException("unknown target domain");
        }

        var p = transform.transform(ap, dv, ml);
        var rap = this.rap * (tfap ^ sfap ? -1 : 1);
        var rml = this.rml * (tfap ^ sfap ? -1 : 1);
        return new ImplantCoordinate(p.getX(), p.getY(), p.getZ(), s, rap, rdv, rml, depth, target);
    }

    /**
     * {@return the insert coordinate in anatomical space}
     */
    public Coordinate insertCoordinate() {
        return new Coordinate(ap, dv, ml);
    }

    /**
     * Get the tip coordinate in anatomical space.
     *
     * @param transform a transformation from probe space to anatomical space.
     * @return The tip coordinate in anatomical space
     * @throws IllegalArgumentException The source domain of {@code transform} does not match to the {@link #reference}.
     */
    public Coordinate tipCoordinate(ProbeTransform<ProbeCoordinate, Coordinate> transform) {
        checkTargetDomain(transform);
        return transform.transform(new ProbeCoordinate(s, 0, 0, 0));
    }

    /**
     * Get the coordinate at certain depth in anatomical space.
     *
     * @param transform a transformation from probe space to anatomical space.
     * @param depth     the distance from the inertion point.
     * @return a coordinate in anatomical space
     * @throws IllegalArgumentException The source domain of {@code transform} does not match to the {@link #reference}.
     */
    public Coordinate toCoordinate(ProbeTransform<ProbeCoordinate, Coordinate> transform, double depth) {
        checkTargetDomain(transform);
        return transform.transform(new ProbeCoordinate(s, 0, this.depth - depth, 0));
    }


    /**
     * Change the insertion point to the coordinate of the first shank.
     *
     * @param coor      shank coordinate
     * @param transform a transformation from probe space to anatomical space.
     * @return an new implant coordinate
     * @throws IllegalArgumentException The source domain of {@code transform} does not match to the {@link #reference}.
     */
    public ImplantCoordinate toShank0(ShankCoordinate coor, ProbeTransform<ProbeCoordinate, Coordinate> transform) {
        return toShank(0, coor, transform);
    }

    /**
     * Change the insertion point to the coordinate of the given shank.
     *
     * @param shank     shank number
     * @param coor      shank coordinate
     * @param transform a transformation from probe space to anatomical space.
     * @return an new implant coordinate
     * @throws IllegalArgumentException The source domain of {@code transform} does not match to the {@link #reference}.
     */
    public ImplantCoordinate toShank(int shank, ShankCoordinate coor, ProbeTransform<ProbeCoordinate, Coordinate> transform) {
        checkTargetDomain(transform);
        if (s == shank) return this;
        return offset(transform.transform(coor.toShank(s, shank)));
    }

    /**
     * offset the insertion position.
     *
     * @param ap ap offset in um.
     * @param dv dv offset in um.
     * @param ml ml offset in um.
     * @return new implant coordinate
     */
    public ImplantCoordinate offset(double ap, double dv, double ml) {
        return new ImplantCoordinate(this.ap + ap, this.dv + dv, this.ml + ml, s, rap, rdv, rml, depth, reference);
    }

    /**
     * offset the insertion position.
     *
     * @param offset offset in um.
     * @return a new implant coordinate
     */
    public ImplantCoordinate offset(Coordinate offset) {
        return offset(offset.ap(), offset.dv(), offset.ml());
    }

    /**
     * Get the rotation of this implant coordinate.
     *
     * @return rotation in radian. Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate rotation() {
        var rap = Math.toRadians(this.rap);
        var rdv = Math.toRadians(this.rdv);
        var rml = Math.toRadians(this.rml);
        return new Coordinate(rap, rdv, rml);
    }

    /**
     * Set the rotation of this implant coordinate. Use insertion point ({@link #insertCoordinate()})
     * as rotation fixed point.
     *
     * @param rotation rotation in radian. Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     * @return a new implant coordinate
     */
    public ImplantCoordinate withRotation(Coordinate rotation) {
        var rap = Math.toDegrees(rotation.ap());
        var rdv = Math.toDegrees(rotation.dv());
        var rml = Math.toDegrees(rotation.ml());
        return new ImplantCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }

    /**
     * Apply the rotation of this implant coordinate. Use insertion point ({@link #insertCoordinate()})
     * as rotation fixed point.
     *
     * @param rotation rotation in radian. Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     * @return a new implant coordinate
     */
    public ImplantCoordinate rotate(Coordinate rotation) {
        var rap = Math.toDegrees(rotation.ap()) + this.rap;
        var rdv = Math.toDegrees(rotation.dv()) + this.rdv;
        var rml = Math.toDegrees(rotation.ml()) + this.rml;
        return new ImplantCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }

    /**
     * Change depth of implant coordinate.
     *
     * @param depth depth from insertion point.
     * @return a new implant coordinate
     */
    public ImplantCoordinate withDepth(double depth) {
        return new ImplantCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }
}
