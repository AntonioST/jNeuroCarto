package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Probe profile.
///
/// References:
/// * [open-ephys-plugins](https://github.com/open-ephys-plugins/neuropixels-pxi/blob/master/Source/Probes/Geometry.cpp#L27)
/// * [SGLXMetaToCoords](https://github.com/jenniferColonell/SGLXMetaToCoords/blob/140452d43a55ea7c7904f09e03858bfe0d499df3/SGLXMetaToCoords.py#L79)
/// * [SpikeGLX](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl.cpp#L115)
@NullMarked
public sealed interface NpxProbeType {

    NpxProbeType np0 = new NP1();
    NpxProbeType np21 = new NP21(21);
    NpxProbeType np24 = new NP24(24);
    NpxProbeType np1020 = new NP1020();
    NpxProbeType np1022 = new NP1022();
    NpxProbeType np1030 = new NP1030();
    NpxProbeType np1032 = new NP1032();
    NpxProbeType np1100 = new NP1100();
    NpxProbeType np1110 = new NP1110();
    NpxProbeType np1120 = new NP1120();
    NpxProbeType np1121 = new NP1121();
    NpxProbeType np1122 = new NP1122();
    NpxProbeType np1123 = new NP1123();
    NpxProbeType np1200 = new NP1200();
    NpxProbeType np1300 = new NP1300();
    NpxProbeType np2000 = new NP21(2000);
    NpxProbeType np2003 = new NP2003();
    NpxProbeType np2010 = new NP24(2010);
    NpxProbeType np2013 = new NP2013();
    NpxProbeType np2020 = new NP2020();
    NpxProbeType np3000 = new NP3000();
    NpxProbeType np3010 = new NP3010();
    NpxProbeType np3020 = new NP3020();

    /// Get [NpxProbeType] by probe type code.
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl.cpp#L1112)
    static NpxProbeType of(int code) {
        return switch (code) {
            case 0, 1000, 1001, 1010, 1011, 1012, 1013, 1014, 1015, 1016, 1017 -> np0;
            case 21 -> np21;
            case 24 -> np24;
            case 1020, 1021 -> np1020;
            case 1022 -> np1022;
            case 1030, 1031 -> np1030;
            case 1032, 1033 -> np1032;
            case 1100 -> np1100;
            case 1110 -> np1110;
            case 1120 -> np1120;
            case 1121 -> np1121;
            case 1122 -> np1122;
            case 1123 -> np1123;
            case 1200, 1210, 1221 -> np1200;
            case 1300 -> np1300;
            case 2000 -> np2000;
            case 2003, 2004, 2005, 2006 -> np2003;
            case 2010 -> np2010;
            case 2013, 2014 -> np2013;
            case 2020, 2021 -> np2020;
            case 3000 -> np3000;
            case 3010, 3011 -> np3010;
            case 3020, 3021, 3022 -> np3020;
            default -> throw new IllegalArgumentException("unknown Neuropixels probe code : " + code);
        };
    }

    /// Get [NpxProbeType] by probe type name.
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl.cpp#L1112)
    static NpxProbeType of(String code) {
        if (code.startsWith("PRB_1_4") || code.startsWith("PRB_1_2")) {
            // PRB_1_4_0480_1 (Silicon cap)
            // PRB_1_4_0480_1_C (Metal cap)
            // PRB_1_2_0480_2
            return np0;
        } else if (code.startsWith("PRB2_1")) {
            // PRB2_1_2_0640_0 (NP 2.0 SS scrambled el 1280)
            return np21;
        } else if (code.startsWith("PRB2_4")) {
            // PRB2_4_2_0640_0 (NP 2.0 MS el 1280)
            return np24;
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

    default int @Nullable [] reference() {
        return null;
    }

    /// Neuropixels 1 based probes.
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

    /// Neuropixels 1.
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T0.h#L12)
    final class NP1 extends NP1Base {
        @Override
        public int code() {
            return 0;
        }

        @Override
        public int[] reference() {
            return new int[]{192, 576, 960};
        }
    }

    /// Neuropixels 2 based probes.
    sealed interface NP21Base extends NpxProbeType {

        @Override
        default int nShank() {
            return 1;
        }

        @Override
        default int nColumnPerShank() {
            return 2;
        }


        @Override
        default int nElectrodePerShank() {
            return 1280;
        }

        @Override
        default int nChannel() {
            return 384;
        }

        @Override
        default float spacePerColumn() {
            return 32;
        }

        @Override
        default float spacePerRow() {
            return 15;
        }

        @Override
        default int spacePerShank() {
            return 0;
        }

        @Override
        default int nReference() {
            return 6;
        }
    }

    /// Neuropixels 2.
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T21.h#L12)
    record NP21(int code) implements NP21Base {
        public NP21 {
            if (code != 21 && code != 2000) {
                throw new IllegalArgumentException("NP21 does not allow alter type code: " + code);
            }
        }

        @Override
        public int[] reference() {
            return new int[]{127, 507, 887, 1251};
        }
    }

    /// 4 shank Neuropixels 2 based probes.
    sealed interface NP24Base extends NpxProbeType {
        @Override
        default int nShank() {
            return 4;
        }

        @Override
        default int nColumnPerShank() {
            return 2;
        }

        @Override
        default int nElectrodePerShank() {
            return 1280;
        }

        @Override
        default int nChannel() {
            return 384;
        }

        @Override
        default float spacePerColumn() {
            return 32;
        }

        @Override
        default float spacePerRow() {
            return 15;
        }

        @Override
        default int spacePerShank() {
            return 250;
        }

        @Override
        default int nReference() {
            return 21;
        }
    }

    /// 4 shank Neuropixels 2.
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T24.h#L12)
    record NP24(int code) implements NP24Base {
        public NP24 {
            if (code != 24 && code != 2010) {
                throw new IllegalArgumentException("NP24 does not allow alter type code: " + code);
            }
        }


        @Override
        public int[] reference() {
            return new int[]{127, 511, 895, 1279};
        }
    }

    /// NHP phase 2
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1020.h#L12)
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

    /// NHP phase 2
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1020.h#L12)
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

    /// NHP phase 2 45 mm
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1030.h#L12)
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

    /// NHP phase 2
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1030.h#L12)
    final class NP1032 extends NP1Base {
        @Override
        public int code() {
            return 1030;
        }

        @Override
        public String name() {
            return "NP1032";
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

    /// UHD phase 1
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1100.h#L12)
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

    /// UHD phase 2
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1110.h#L61)
    final class NP1110 extends NP1Base {

        public enum Mode {
            INNER, OUTER, ALL;
        }

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

    /// UHD phase 3
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1120.h#L12)
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

    /// UHD phase 3
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1121.h#L12)
    final class NP1121 extends NP1Base {
        @Override
        public int code() {
            return 1121;
        }

        @Override
        public int nColumnPerShank() {
            return 1;
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

    /// UHD phase 3
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1122.h#L12)
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

    /// UHD phase 3
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1123.h#L12)
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

    /// NHP 128
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1200.h#L12)
    final class NP1200 extends NP1Base {
        @Override
        public int code() {
            return 1200;
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
        public float spacePerRow() {
            return 31;
        }

        @Override
        public int nReference() {
            return 2;
        }
    }

    /// Opto
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T1300.h#L12)
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

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T2003.h#L12)
    final class NP2003 implements NP21Base {
        @Override
        public int code() {
            return 2003;
        }

        @Override
        public int nReference() {
            return 3;
        }
    }

    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T2013.h#L12)
    final class NP2013 implements NP24Base {
        @Override
        public int code() {
            return 2013;
        }

        @Override
        public int nReference() {
            return 6;
        }
    }

    /// Neuropixels 2.0 quad base
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T2020.h#L43)
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

    /// Passive NXT probe
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

    /// NXT single shank
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T3010base.h#L47)
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T3010.h#L12)
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

    /// NXT multishank
    ///
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T3020base.h#L47)
    /// [reference](https://github.com/billkarsh/SpikeGLX/blob/bc2c10e99e68dcc9ec6b9a9c75272a74c7e53034/Src-imro/IMROTbl_T3020.h#L12)
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
