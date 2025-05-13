package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * @param ap        ap insertion position in um
 * @param dv        dv insertion position in um
 * @param ml        ml insertion position in um
 * @param s         the shank of the insertion position.
 * @param rap       rotation along ap-axis in degree
 * @param rdv       rotation along dv-axis in degree
 * @param rml       rotation along ml-axis in degree
 * @param depth     insert depth
 * @param reference insertion position reference.
 */
@NullMarked
public record ProbeCoordinate(
  double ap,
  double dv,
  double ml,
  int s,
  double rap,
  double rdv,
  double rml,
  double depth,
  @Nullable Coordinate reference
) {

    public ProbeCoordinate {
        if (s < 0) throw new IllegalArgumentException("negative shank : " + s);
        rap %= 360;
        rdv %= 360;
        rml %= 360;
    }

    public ProbeCoordinate(double ap, double ml) {
        this(ap, 0, ml, 0, 0, 0, 0, 0, null);
    }

    public ProbeCoordinate(double ap, double ml, double depth) {
        this(ap, 0, ml, 0, 0, 0, 0, depth, null);
    }

    public Coordinate insertCoordinate() {
        return new Coordinate(ap, dv, ml);
    }

    public Coordinate tipCoordinate() {
        return new Coordinate(ap, dv + depth, ml);
    }

    public Coordinate toCoordinate(double depth) {
        return new Coordinate(ap, dv + depth, ml);
    }


    public ProbeCoordinate toOrigin() {
        if (reference == null) return this;

        return new ProbeCoordinate(
          reference.ap() - ap,
          reference.dv() + dv,
          reference.ml() + ml,
          s, -rap, -rdv, -rml, depth, null
        );
    }

    public ProbeCoordinate toReference(Coordinate reference) {
        var origin = toOrigin();
        return new ProbeCoordinate(
          reference.ap() - origin.ap,
          origin.dv - reference.dv(),
          origin.ml - reference.ml(),
          origin.s,
          -origin.rap, -origin.rdv, -origin.rml,
          origin.depth,
          reference
        );
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
    public ProbeCoordinate setRotation(Coordinate rotation) {
        var rap = Math.toDegrees(rotation.ap());
        var rdv = Math.toDegrees(rotation.dv());
        var rml = Math.toDegrees(rotation.ml());
        return new ProbeCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);
    }

    /**
     * @param rotation Reuse {@link Coordinate} but changing fields' meaning to roration radians.
     * @return
     */
    public ProbeCoordinate withRotation(Coordinate rotation) {
        var rap = Math.toDegrees(rotation.ap()) + this.rap;
        var rdv = Math.toDegrees(rotation.dv()) + this.rdv;
        var rml = Math.toDegrees(rotation.ml()) + this.rml;
        return new ProbeCoordinate(ap, dv, ml, s, rap, rdv, rml, depth, reference);

    }
}
