package io.ast.jneurocarto.core;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/**
 * An electrode interface for GUI interaction between different electrode implementations.
 */
@NullMarked
public final class ElectrodeDescription {
    private final int s;
    private final int x;
    private final int y;
    private final Object electrode;
    private final Object channel;
    private int state = ProbeDescription.STATE_UNUSED;
    private int category = ProbeDescription.CATE_UNSET;

    /**
     * @param s         shank
     * @param x         x position in um
     * @param y         y position in um
     * @param electrode electrode identify. It should be a hashable.
     * @param channel   channel identify. It is used for display (str-able).
     */
    public ElectrodeDescription(int s, int x, int y, Object electrode, Object channel) {
        this.s = s;
        this.x = x;
        this.y = y;
        this.electrode = electrode;
        this.channel = channel;
    }

    /**
     * @param s         shank
     * @param x         x position in um
     * @param y         y position in um
     * @param electrode electrode identify. It should be a hashable.
     * @param channel   channel identify. It is used for display (str-able).
     * @param state     electrode selecting state
     * @param category  electrode selecting category.
     */
    public ElectrodeDescription(int s, int x, int y, Object electrode, Object channel, int state, int category) {
        this.s = s;
        this.x = x;
        this.y = y;
        this.electrode = electrode;
        this.channel = channel;
        this.state = state;
        this.category = category;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(electrode);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof ElectrodeDescription ed) && Objects.equals(electrode, ed.electrode);
    }

    @Override
    public String toString() {
        return "Electrode[" + electrode + "]";
    }

    public int s() {
        return s;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public Object electrode() {
        return electrode;
    }

    public Object channel() {
        return channel;
    }

    /**
     * {@return electrode selecting state}
     */
    public int state() {
        return state;
    }

    public void state(int state) {
        this.state = state;
    }

    /**
     * {@return electrode selecting category.}
     */
    public int category() {
        return category;
    }

    public void category(int category) {
        this.category = category;
    }

}
