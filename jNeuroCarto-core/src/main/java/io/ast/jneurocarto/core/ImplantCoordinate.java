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

    public ImplantCoordinate(double ap, double ml) {
        this(ap, 0, ml, 0, 0, 0, 0, 0, null);
    }

    public ImplantCoordinate(double ap, double ml, double depth) {
        this(ap, 0, ml, 0, 0, 0, 0, depth, null);
    }

    private void checkSourceDomain(ProbeTransform<Coordinate, ?> transform) {
        if (reference == null) {
            if (!(transform.sourceDomain() instanceof ProbeTransform.Anatomical)) {
                throw new RuntimeException("source domain is not a global anatomical space");
            }
        } else {
            if (!(transform.sourceDomain() instanceof ProbeTransform.ReferencedAnatomical(var reference, _, _))) {
                throw new RuntimeException("source domain is not a referenced anatomical space");
            } else if (!this.reference.equals(reference)) {
                throw new RuntimeException("source referenced domain does not match to the " + this.reference + " reference");
            }
        }
    }

    private void checkTargetDomain(ProbeTransform<?, Coordinate> transform) {
        if (reference == null) {
            if (!(transform.targetDomain() instanceof ProbeTransform.Anatomical)) {
                throw new RuntimeException("target domain is not a global anatomical space");
            }
        } else {
            if (!(transform.targetDomain() instanceof ProbeTransform.ReferencedAnatomical(var ref, _, _))) {
                throw new RuntimeException("target domain is not a referenced anatomical space");
            } else if (!reference.equals(ref)) {
                throw new RuntimeException("target referenced domain does not match to the " + reference + " reference");
            }
        }
    }

    /**
     * @param transform a {@link ProbeTransform}, which its source domain should match to {@link #reference}.
     * @return a new implant coordinate with the reference match to {@code transform}'s target domain.
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
        default -> throw new RuntimeException("unknown target domain");
        }

        var p = transform.transform(ap, dv, ml);
        var rap = this.rap * (tfap ^ sfap ? -1 : 1);
        var rml = this.rml * (tfap ^ sfap ? -1 : 1);
        return new ImplantCoordinate(p.getX(), p.getY(), p.getZ(), s, rap, rdv, rml, depth, target);
    }

    public Coordinate insertCoordinate() {
        return new Coordinate(ap, dv, ml);
    }

    public Coordinate tipCoordinate(ProbeTransform<ProbeCoordinate, Coordinate> transform) {
        checkTargetDomain(transform);
        return transform.transform(new ProbeCoordinate(s, 0, 0, 0));
    }


    public Coordinate toCoordinate(ProbeTransform<ProbeCoordinate, Coordinate> transform, double depth) {
        checkTargetDomain(transform);
        return transform.transform(new ProbeCoordinate(s, 0, this.depth - depth, 0));
    }


    public ImplantCoordinate toShank0(ShankCoordinate coor, ProbeTransform<ProbeCoordinate, Coordinate> transform) {
        return toShank(0, coor, transform);
    }

    public ImplantCoordinate toShank(int shank, ShankCoordinate coor, ProbeTransform<ProbeCoordinate, Coordinate> transform) {
        checkTargetDomain(transform);
        if (s == shank) return this;
        return offset(transform.transform(coor.toShank(s, shank)));
    }

    /**
     * offset the insertion position. If the {@link #reference} is not null, the direction {@code ap} will be flipped.
     *
     * @param ap ap offset in um, follow global AP-axis.
     * @param dv dv offset in um, follow global DV-axis.
     * @param ml ml offset in um, follow global ML-axis.
     * @return new insertion position.
     */
    public ImplantCoordinate offset(double ap, double dv, double ml) {
        return new ImplantCoordinate(this.ap + ap, this.dv + dv, this.ml + ml, s, rap, rdv, rml, depth, reference);
    }

    public ImplantCoordinate offset(Coordinate offset) {
        return offset(offset.ap(), offset.dv(), offset.ml());
    }

    /**
     * @return rotation. Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     */
    public Coordinate rotation() {
        var rap = Math.toRadians(this.rap);
        var rdv = Math.toRadians(this.rdv);
        var rml = Math.toRadians(this.rml);
        return new Coordinate(rap, rdv, rml);
    }

    /**
     * @param rotation Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     * @return
     */
    public ImplantCoordinate withRotation(Coordinate rotation) {
        var rap = Math.toDegrees(rotation.ap());
        var rdv = Math.toDegrees(rotation.dv());
        var rml = Math.toDegrees(rotation.ml());
        return new ImplantCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }

    /**
     * @param rotation Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     * @return
     */
    public ImplantCoordinate rotate(Coordinate rotation) {
        var rap = Math.toDegrees(rotation.ap()) + this.rap;
        var rdv = Math.toDegrees(rotation.dv()) + this.rdv;
        var rml = Math.toDegrees(rotation.ml()) + this.rml;
        return new ImplantCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }

    public ImplantCoordinate withDepth(double depth) {
        return new ImplantCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }
}
