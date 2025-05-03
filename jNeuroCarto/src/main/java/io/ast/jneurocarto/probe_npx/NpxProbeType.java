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

    public int code() {
        return info.code();
    }

    public static NpxProbeType of(int code) {
        return switch (code) {
            case 0 -> NP1;
            case 21 -> NP21;
            case 24 -> NP24;
            default -> throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
        };
    }

    public static NpxProbeType of(String code) {
        return switch (code) {
            case "0" -> NP1;
            case "21", "NP2_1", "PRB2_1_2_0640_0", "PRB2_1_4_0480_1", "NP2000", "NP2003", "NP2004" -> NP21;
            case "24", "NP2_4", "PRB2_4_2_0640_0", "NP2010", "NP2013", "NP2014" -> NP24;
            default -> throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
        };
    }
}
