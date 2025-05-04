package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

/// Probe profile.
///
/// References:
/// * [open-ephys-plugins](https://github.com/open-ephys-plugins/neuropixels-pxi/blob/master/Source/Probes/Geometry.cpp#L27)
/// * [SpikeGLX](https://github.com/jenniferColonell/SGLXMetaToCoords/blob/140452d43a55ea7c7904f09e03858bfe0d499df3/SGLXMetaToCoords.py#L79)
@NullMarked
public sealed interface NpxProbeType {

    NpxProbeType NP1 = new NP1();
    NpxProbeType NP21 = new NP21();
    NpxProbeType NP24 = new NP24();

    static NpxProbeType of(int code) {
        return switch (code) {
            case 0 -> NP1;
            case 21 -> NP21;
            case 24 -> NP24;
            default -> throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
        };
    }

    static NpxProbeType of(String code) {
        return switch (code) {
            case "0" -> NP1;
            case "21", "NP2_1", "PRB2_1_2_0640_0", "PRB2_1_4_0480_1", "NP2000", "NP2003", "NP2004" -> NP21;
            case "24", "NP2_4", "PRB2_4_2_0640_0", "NP2010", "NP2013", "NP2014" -> NP24;
            default -> throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
        };
    }

    int code();

    /**
     * {@return number of shank}
     */
    int nShank();

    /**
     * {@return number of columns per shank}
     */
    int nColumnPerShank();

    /**
     * {@return number of rows per shank}
     */
    int nRowPerShank();

    /**
     * {@return number of electrode per shank. It is equals to `n_col_shank * n_row_shank`.}
     */
    int nElectrodePerShank();

    /**
     * {@return number of total channels.}
     */
    int nChannel();

    /**
     * {@return number of electrode per block.}
     */
    int nElectrodePerBlock();

    /**
     * {@return electrodes column space, um}
     */
    int spacePerColumn();

    /**
     * {@return electrodes row space, um}
     */
    int spacePerRow();

    /**
     * {@return shank space, um}
     */
    int spacePerShank();

    /**
     * {@return }
     */
    int[] reference();

    default int nBank() {
        return (int) Math.ceil((double) nElectrodePerShank() / nChannel());
    }

    default int nBlock() {
        return nElectrodePerShank() / nElectrodePerBlock();
    }

    default int nBlockPerBank() {
        return nChannel() / nElectrodePerBlock();
    }

    final class NP1 implements NpxProbeType {
        @Override
        public int code() {
            return 0;
        }

        @Override
        public int nShank() {
            return 1;
        }

        @Override
        public int nColumnPerShank() {
            return 2;
        }

        @Override
        public int nRowPerShank() {
            return 480;
        }

        @Override
        public int nElectrodePerShank() {
            return 960;
        }

        @Override
        public int nChannel() {
            return 384;
        }

        @Override
        public int nElectrodePerBlock() {
            return 32;
        }

        @Override
        public int spacePerColumn() {
            return 32;
        }

        @Override
        public int spacePerRow() {
            return 20;
        }

        @Override
        public int spacePerShank() {
            return 0;
        }

        @Override
        public int[] reference() {
            return new int[]{192, 576, 960};
        }
    }

    final class NP21 implements NpxProbeType {
        @Override
        public int code() {
            return 21;
        }

        @Override
        public int nShank() {
            return 1;
        }

        @Override
        public int nColumnPerShank() {
            return 2;
        }

        @Override
        public int nRowPerShank() {
            return 640;
        }

        @Override
        public int nElectrodePerShank() {
            return 1280;
        }

        @Override
        public int nChannel() {
            return 384;
        }

        @Override
        public int nElectrodePerBlock() {
            return 48;
        }

        @Override
        public int spacePerColumn() {
            return 32;
        }

        @Override
        public int spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 0;
        }

        @Override
        public int[] reference() {
            return new int[]{127, 507, 887, 1251};
        }
    }

    final class NP24 implements NpxProbeType {
        @Override
        public int code() {
            return 24;
        }

        @Override
        public int nShank() {
            return 4;
        }

        @Override
        public int nColumnPerShank() {
            return 2;
        }

        @Override
        public int nRowPerShank() {
            return 640;
        }

        @Override
        public int nElectrodePerShank() {
            return 1280;
        }

        @Override
        public int nChannel() {
            return 384;
        }

        @Override
        public int nElectrodePerBlock() {
            return 48;
        }

        @Override
        public int spacePerColumn() {
            return 32;
        }

        @Override
        public int spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 250;
        }

        @Override
        public int[] reference() {
            return new int[]{127, 511, 895, 1279};
        }
    }
}
