package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

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

    public static ReferenceInfo of(NpxProbeType type, int reference) {
        if (reference < 0 || reference >= type.nReference()) {
            throw new IllegalArgumentException("reference id out of boundary for probe type " + type.code() + ": " + reference);
        }

        if (reference == 0) return new ReferenceInfo(0, ReferenceType.EXT, 0, 0);

        return switch (type.code()) {
            case 2003, 2013, 2020, 3010, 3020 -> {
                if (reference == 1) yield new ReferenceInfo(reference, ReferenceType.GROUND, 0, 0);
                if (reference - 2 == type.nShank()) yield new ReferenceInfo(reference, ReferenceType.TIP, reference - 2, 0);
                // XXX probe 2013 has 7 references, but SpikeGLX only said what 6 is.
                yield new ReferenceInfo(reference, ReferenceType.UNKNOWN, 0, 0);
            }
            case 21 -> {
                if (reference == 1) yield new ReferenceInfo(reference, ReferenceType.TIP, 0, 0);
                if (reference - 1 < type.nShank()) {
                    yield new ReferenceInfo(reference, ReferenceType.BANK, reference - 1, ((NpxProbeType.NP21) type).reference()[reference - 1]);
                }
                throw new IllegalArgumentException();
            }
            case 24 -> {
                if (reference - 1 < type.nShank()) {
                    yield new ReferenceInfo(reference, ReferenceType.TIP, reference - 1, 0);
                }
                var r = reference - 1 - type.nShank();
                var rs = ((NpxProbeType.NP24) type).reference();
                var shank = r / rs.length;
                var index = r % rs.length;
                yield new ReferenceInfo(reference, ReferenceType.BANK, shank, rs[index]);
            }
            default -> {
                assert type.nShank() == 1;
                if (reference == 1) yield new ReferenceInfo(reference, ReferenceType.TIP, 0, 0);
                yield new ReferenceInfo(reference, ReferenceType.BANK, reference - 2, 0);
            }
        };
    }
}
