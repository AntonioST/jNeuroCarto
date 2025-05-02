package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum NpxProbeType {
    NP1(new NpxProbeInfo(0, 1, 2, 480, 960, 384, 32, 32, 20, 0, new int[]{192, 576, 960})),
    NP21(new NpxProbeInfo(21, 1, 2, 640, 1280, 384, 48, 32, 15, 0, new int[]{127, 507, 887, 1251})),
    NP24(new NpxProbeInfo(24, 4, 2, 640, 1280, 384, 48, 32, 15, 250, new int[]{127, 511, 895, 1279}));

    private final NpxProbeInfo info;

    NpxProbeType(NpxProbeInfo info) {
        this.info = info;
    }

    public NpxProbeInfo info() {
        return info;
    }
}
