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
        EXT, TIP, ON_SHANK;
    }

    public static int maxReferenceValue(NpxProbeType type) {
        return maxReferenceValue(type.info());
    }

    public static int maxReferenceValue(NpxProbeInfo info) {
        var ns = info.nShank();
        var refs = info.reference();
        return 1 + ns + ns * refs.length;
    }

    public static ReferenceInfo of(NpxProbeType type, int reference) {
        return of(type.info(), reference);
    }

    public static ReferenceInfo of(NpxProbeInfo info, int reference) {
        var n = info.nShank();

        if (reference == 0) {
            return new ReferenceInfo(0, ReferenceType.EXT, 0, 0);
        } else if (reference < n + 1) {
            return new ReferenceInfo(reference, ReferenceType.TIP, reference - 1, 0);
        }

        var refs = info.reference();
        var r = reference - n - 1;
        var s = r / refs.length;
        var i = r % refs.length;
        var c = refs[i];
        return new ReferenceInfo(reference, ReferenceType.ON_SHANK, s, c);
    }
}
