package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

/**
 *
 * @param code    reference code
 * @param type    reference type
 * @param shank   reference located
 * @param channel reference bind channel
 */
@NullMarked
public record ReferenceInfo(
  int code,
  ReferenceType type,
  int shank,
  int channel
) {
    public enum ReferenceType {
        EXT, TIP, BANK, GROUND, UNKNOWN
    }

    /// Get [ReferenceInfo] for certain probe type with particular reference id.
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl.h#L191)
    ///
    /// @param type      probe type
    /// @param reference reference id.
    /// @throws IllegalArgumentException reference index out of range.
    public static ReferenceInfo of(NpxProbeType type, int reference) {
        if (reference < 0 || reference >= type.nReference()) {
            throw new IllegalArgumentException("reference id out of boundary for probe type " + type.code() + ": " + reference);
        }

        if (reference == 0) return new ReferenceInfo(0, ReferenceType.EXT, 0, 0);

        if (type instanceof NpxProbeType.NP21 np21) {
            if (reference == 1) return new ReferenceInfo(reference, ReferenceType.TIP, 0, 0);
            if (reference - 1 < type.nShank()) {
                assert np21.reference().length == 4;
                return new ReferenceInfo(reference, ReferenceType.BANK, reference - 1, np21.reference()[reference - 1]);
            }
            throw new IllegalArgumentException();
        } else if (type instanceof NpxProbeType.NP24 np24) {
            if (reference - 1 < type.nShank()) {
                return new ReferenceInfo(reference, ReferenceType.TIP, reference - 1, 0);
            }
            var r = reference - 1 - type.nShank();
            assert np24.reference().length == 4;

            var shank = r / 4;
            var index = r % 4;
            return new ReferenceInfo(reference, ReferenceType.BANK, shank, np24.reference()[index]);
        } else {
            return switch (type.code()) {
                case 2003, 2013, 2020, 3010, 3020 -> {
                    if (reference == 1) yield new ReferenceInfo(reference, ReferenceType.GROUND, 0, 0);
                    if (reference - 2 == type.nShank()) yield new ReferenceInfo(reference, ReferenceType.TIP, reference - 2, 0);
                    // XXX probe 2013 has 7 references, but SpikeGLX only said what 6 is.
                    yield new ReferenceInfo(reference, ReferenceType.UNKNOWN, 0, 0);
                }
                default -> {
                    assert type.nShank() == 1;
                    if (reference == 1) yield new ReferenceInfo(reference, ReferenceType.TIP, 0, 0);
                    yield new ReferenceInfo(reference, ReferenceType.BANK, reference - 2, 0);
                }
            };
        }
    }
}
