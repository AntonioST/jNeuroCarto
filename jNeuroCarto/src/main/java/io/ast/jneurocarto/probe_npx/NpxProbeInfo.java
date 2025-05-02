package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

/// Probe profile.
///
/// References:
/// * [open-ephys-plugins](https://github.com/open-ephys-plugins/neuropixels-pxi/blob/master/Source/Probes/Geometry.cpp#L27)
/// * [SpikeGLX](https://github.com/jenniferColonell/SGLXMetaToCoords/blob/140452d43a55ea7c7904f09e03858bfe0d499df3/SGLXMetaToCoords.py#L79)
///
/// @param code
/// @param nShank             number of shank
/// @param nColumnPerShank    number of columns per shank
/// @param nRowPerShank       number of rows per shank
/// @param nElectrodePerShank number of electrode per shank. It is equals to `n_col_shank * n_row_shank`.
/// @param nChannel           number of total channels.
/// @param nElectrodePerBlock number of electrode per block.
/// @param spacePerColumn     electrodes column space, um
/// @param spacePerRow        electrodes row space, um
/// @param spacePerShank      shank space, um
/// @param reference
@NullMarked
public record NpxProbeInfo(
  int code,
  int nShank,
  int nColumnPerShank,
  int nRowPerShank,
  int nElectrodePerShank,
  int nChannel,
  int nElectrodePerBlock,
  int spacePerColumn,
  int spacePerRow,
  int spacePerShank,
  int[] reference
) {

    public int nBank() {
        return (int) Math.ceil((double) nElectrodePerShank / nChannel);
    }

    public int nBlock() {
        return nElectrodePerShank / nElectrodePerBlock;
    }

    public int nBlockPerBank() {
        return nChannel / nElectrodePerBlock;
    }
}
