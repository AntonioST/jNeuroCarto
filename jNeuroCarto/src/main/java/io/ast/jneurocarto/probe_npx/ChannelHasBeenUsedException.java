package io.ast.jneurocarto.probe_npx;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ChannelHasBeenUsedException extends RuntimeException {

    private final ChannelMap chmap;
    private final Electrode electrode;
    private final int shank;
    private final int column;
    private final int row;

    ChannelHasBeenUsedException(ChannelMap chmap, Electrode electrode, int shank, int column, int row) {
        super(electrode + " has been used.");

        this.chmap = chmap;
        this.electrode = electrode;
        this.shank = shank;
        this.column = column;
        this.row = row;
    }

    public Electrode getElectrode() {
        return electrode;
    }

    public Electrode forceAddElectrode() {
        synchronized (chmap) {
            chmap.removeElectrode(electrode);
            return chmap.addElectrode(shank, column, row);
        }
    }
}
