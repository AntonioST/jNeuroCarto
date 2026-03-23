package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

/// Probe profile.
///
/// References:
/// * [open-ephys-plugins](https://github.com/open-ephys-plugins/neuropixels-pxi/blob/master/Source/Probes/Geometry.cpp#L27)
/// * [SpikeGLX](https://github.com/jenniferColonell/SGLXMetaToCoords/blob/140452d43a55ea7c7904f09e03858bfe0d499df3/SGLXMetaToCoords.py#L79)
@NullMarked
public sealed interface NpxProbeType {

    NpxProbeType NP0 = new NP1();
    NpxProbeType NP21 = new NP21();
    NpxProbeType NP24 = new NP24();
    NpxProbeType NP1020 = new NP1020();
    NpxProbeType NP1022 = new NP1022();
    NpxProbeType NP1030 = new NP1030();
    NpxProbeType NP1032 = new NP1032();
    NpxProbeType NP1100 = new NP1100();
    NpxProbeType NP1110 = new NP1110();
    NpxProbeType NP1120 = new NP1120();
    NpxProbeType NP1121 = new NP1121();
    NpxProbeType NP1122 = new NP1122();
    NpxProbeType NP1123 = new NP1123();
    NpxProbeType NP1200 = new NP1200();
    NpxProbeType NP1300 = new NP1300();
    NpxProbeType NP2003 = new NP2003();
    NpxProbeType NP2013 = new NP2013();
    NpxProbeType NP2020 = new NP2020();
    NpxProbeType NP3000 = new NP3000();
    NpxProbeType NP3010 = new NP3010();
    NpxProbeType NP3020 = new NP3020();

    static NpxProbeType of(int code) {
        return switch (code) {
            case 0, 1000, 1001, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017 -> NP0;
            case 21, 2000 -> NP21;
            case 24, 2010 -> NP24;
            case 1020, 1021 -> NP1020;
            case 1022 -> NP1022;
            case 1030, 1031 -> NP1030;
            case 1032, 1033 -> NP1032;
            case 1100 -> NP1100;
            case 1110 -> NP1110;
            case 1120 -> NP1120;
            case 1121 -> NP1121;
            case 1122 -> NP1122;
            case 1123 -> NP1123;
            case 1200, 1210, 1221 -> NP1200;
            case 1300 -> NP1300;
            case 2003, 2004, 2005, 2006 -> NP2003;
            case 2013, 2014 -> NP2013;
            case 2020, 2021 -> NP2020;
            case 3000 -> NP3000;
            case 3010, 3011 -> NP3010;
            case 3020, 3021 -> NP3020;
            default -> throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
        };
    }

    static NpxProbeType of(String code) {
        if (code.startsWith("PRB_1_4") || code.startsWith("PRB_1_2")) {
            return NP0;
        } else if (code.startsWith("PRB2_1")) {
            return NP21;
        } else if (code.startsWith("PRB2_4")) {
            return NP24;
        } else if (code.startsWith("NP")) {
            return of(Integer.parseInt(code.substring(2)));
        }
        throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
    }

    int code();

    default String name() {
        return "NP" + code();
    }

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
    default int nRowPerShank() {
        return nElectrodePerShank() / nColumnPerShank();
    }

    /**
     * {@return number of electrode per shank. It is equals to `n_col_shank * n_row_shank`.}
     */
    int nElectrodePerShank();

    /**
     * {@return number of total channels.}
     */
    int nChannel();

    /**
     * {@return electrodes column space, um}
     */
    float spacePerColumn();

    /**
     * {@return electrodes row space, um}
     */
    float spacePerRow();

    /**
     * {@return shank space, um}
     */
    int spacePerShank();

    int nReference();

    default int nBank() {
        return (int) Math.ceil((double) nElectrodePerShank() / nChannel());
    }

    default int nElectrode() {
        return nElectrodePerShank() * nShank();
    }

    abstract sealed class NP1Base implements NpxProbeType {

        @Override
        public int nShank() {
            return 1;
        }

        @Override
        public int nColumnPerShank() {
            return 2;
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
        public float spacePerColumn() {
            return 32;
        }

        @Override
        public float spacePerRow() {
            return 20;
        }

        @Override
        public int spacePerShank() {
            return 0;
        }

        @Override
        public int nReference() {
            return 5;
        }


    }

    final class NP1 extends NP1Base {
        @Override
        public int code() {
            return 0;
        }

//        public int[] reference() {
//            return new int[]{192, 576, 960};
//        }
    }

    abstract sealed class NP21Base implements NpxProbeType {

        @Override
        public int nShank() {
            return 1;
        }

        @Override
        public int nColumnPerShank() {
            return 2;
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
        public float spacePerColumn() {
            return 32;
        }

        @Override
        public float spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 0;
        }

        @Override
        public int nReference() {
            return 6;
        }
    }

    final class NP21 extends NP21Base {
        @Override
        public int code() {
            return 21;
        }

        public int[] reference() {
            return new int[]{127, 507, 887, 1251};
        }
    }

    abstract sealed class NP24Base implements NpxProbeType {
        @Override
        public int nShank() {
            return 4;
        }

        @Override
        public int nColumnPerShank() {
            return 2;
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
        public float spacePerColumn() {
            return 32;
        }

        @Override
        public float spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 250;
        }

        @Override
        public int nReference() {
            return 18;
        }


    }

    final class NP24 extends NP24Base {
        @Override
        public int code() {
            return 24;
        }

        public int[] reference() {
            return new int[]{127, 511, 895, 1279};
        }
    }

    final class NP1020 extends NP1Base {
        @Override
        public int code() {
            return 1020;
        }


        @Override
        public int nElectrodePerShank() {
            return 2496;
        }

        @Override
        public float spacePerColumn() {
            return 87;
        }

        @Override
        public int nReference() {
            return 9;
        }
    }

    final class NP1022 extends NP1Base {
        @Override
        public int code() {
            return 1020;
        }


        @Override
        public int nElectrodePerShank() {
            return 2496;
        }

        @Override
        public float spacePerColumn() {
            return 103;
        }

        @Override
        public int nReference() {
            return 9;
        }
    }

    final class NP1030 extends NP1Base {
        @Override
        public int code() {
            return 1030;
        }


        @Override
        public int nElectrodePerShank() {
            return 4416;
        }

        @Override
        public float spacePerColumn() {
            return 87;
        }

        @Override
        public int nReference() {
            return 14;
        }
    }

    final class NP1032 extends NP1Base {
        @Override
        public int code() {
            return 1032;
        }


        @Override
        public int nElectrodePerShank() {
            return 4416;
        }

        @Override
        public float spacePerColumn() {
            return 103;
        }

        @Override
        public int nReference() {
            return 14;
        }
    }

    final class NP1100 extends NP1Base {
        @Override
        public int code() {
            return 1100;
        }


        @Override
        public int nColumnPerShank() {
            return 8;
        }

        @Override
        public int nElectrodePerShank() {
            return 384;
        }

        @Override
        public float spacePerColumn() {
            return 6;
        }

        @Override
        public float spacePerRow() {
            return 6;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1110 extends NP1Base {
        @Override
        public int code() {
            return 1110;
        }


        @Override
        public int nColumnPerShank() {
            return 8;
        }

        @Override
        public int nElectrodePerShank() {
            return 6144;
        }

        @Override
        public float spacePerColumn() {
            return 6;
        }

        @Override
        public float spacePerRow() {
            return 6;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1120 extends NP1Base {
        @Override
        public int code() {
            return 1120;
        }


        @Override
        public int nElectrodePerShank() {
            return 384;
        }

        @Override
        public float spacePerColumn() {
            return 4.5F;
        }

        @Override
        public float spacePerRow() {
            return 4.5F;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1121 extends NP1Base {
        @Override
        public int code() {
            return 1121;
        }

        @Override
        public int nElectrodePerShank() {
            return 384;
        }

        @Override
        public float spacePerColumn() {
            return 3;
        }

        @Override
        public float spacePerRow() {
            return 3;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1122 extends NP1Base {
        @Override
        public int code() {
            return 1122;
        }

        @Override
        public int nColumnPerShank() {
            return 16;
        }

        @Override
        public int nElectrodePerShank() {
            return 384;
        }

        @Override
        public float spacePerColumn() {
            return 3;
        }

        @Override
        public float spacePerRow() {
            return 3;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1123 extends NP1Base {
        @Override
        public int code() {
            return 1123;
        }

        @Override
        public int nColumnPerShank() {
            return 12;
        }

        @Override
        public int nElectrodePerShank() {
            return 384;
        }

        @Override
        public float spacePerColumn() {
            return 4.5F;
        }

        @Override
        public float spacePerRow() {
            return 4.5F;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1200 extends NP1Base {
        @Override
        public int code() {
            return 1200;
        }

        @Override
        public int nElectrodePerShank() {
            return 384;
        }

        @Override
        public float spacePerRow() {
            return 31;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    final class NP1300 extends NP1Base {
        @Override
        public int code() {
            return 1300;
        }

        @Override
        public float spacePerColumn() {
            return 48;
        }
    }

    final class NP2003 extends NP21Base {
        @Override
        public int code() {
            return 2003;
        }

        @Override
        public float spacePerColumn() {
            return 48;
        }

        @Override
        public int nReference() {
            return 3;
        }
    }

    final class NP2013 extends NP24Base {
        @Override
        public int code() {
            return 2013;
        }

        @Override
        public int nReference() {
            return 4;
        }
    }

    final class NP2020 implements NpxProbeType {
        @Override
        public int code() {
            return 2020;
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
        public int nElectrodePerShank() {
            return 1280;
        }

        @Override
        public int nChannel() {
            return 1536;
        }

        @Override
        public float spacePerColumn() {
            return 32;
        }

        @Override
        public float spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 250;
        }

        @Override
        public int nReference() {
            return 3;
        }

    }

    final class NP3000 implements NpxProbeType {
        @Override
        public int code() {
            return 3000;
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
        public int nElectrodePerShank() {
            return 128;
        }

        @Override
        public int nChannel() {
            return 128;
        }

        @Override
        public float spacePerColumn() {
            return 15;
        }

        @Override
        public float spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 0;
        }

        @Override
        public int nReference() {
            return 2;
        }

    }

    final class NP3010 implements NpxProbeType {
        @Override
        public int code() {
            return 3010;
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
        public int nElectrodePerShank() {
            return 1280;
        }

        @Override
        public int nChannel() {
            return 912;
        }

        @Override
        public float spacePerColumn() {
            return 32;
        }

        @Override
        public float spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 0;
        }

        @Override
        public int nReference() {
            return 3;
        }

    }

    final class NP3020 implements NpxProbeType {
        @Override
        public int code() {
            return 3020;
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
        public int nElectrodePerShank() {
            return 1280;
        }

        @Override
        public int nChannel() {
            return 912;
        }

        @Override
        public float spacePerColumn() {
            return 32;
        }

        @Override
        public float spacePerRow() {
            return 15;
        }

        @Override
        public int spacePerShank() {
            return 250;
        }

        @Override
        public int nReference() {
            return 7;
        }

    }
}
