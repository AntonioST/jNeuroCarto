package io.ast.jneurocarto.probe_npx;

public class ChannelHasBeenUsedException extends RuntimeException {

    private final Electrode electrode;

    ChannelHasBeenUsedException(Electrode electrode) {
        super(electrode + " has been used.");
        this.electrode = electrode;
    }

    public Electrode getElectrode() {
        return electrode;
    }
}
