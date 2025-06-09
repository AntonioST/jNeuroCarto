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
     * Create an electrode description.
     *
     * @param s         shank
     * @param x         x position in um
     * @param y         y position in um
     * @param electrode electrode identify. It should be a hashable.
     * @param channel   channel identify. It is used for display (str-able).
     * @throws NullPointerException when {@code electrode} is {@code null}
     */
    public ElectrodeDescription(int s, int x, int y, Object electrode, Object channel) {
        this.s = s;
        this.x = x;
        this.y = y;
        this.electrode = Objects.requireNonNull(electrode);
        this.channel = channel;
    }

    /**
     * Create an electrode description with initial category.
     *
     * @param s         shank
     * @param x         x position in um
     * @param y         y position in um
     * @param electrode electrode identify. It should be a hashable.
     * @param channel   channel identify. It is used for display (str-able).
     * @param state     electrode selecting state
     * @param category  electrode selecting category.
     * @throws NullPointerException when {@code electrode} is {@code null}
     */
    public ElectrodeDescription(int s, int x, int y, Object electrode, Object channel, int state, int category) {
        this.s = s;
        this.x = x;
        this.y = y;
        this.electrode = Objects.requireNonNull(electrode);
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
        return "Electrode[" + electrode + ", channel=" + channel + "]";
    }

    /**
     * {@return shank}
     */
    public int s() {
        return s;
    }

    /**
     * {@return x position in um}
     */
    public int x() {
        return x;
    }

    /**
     * {@return y position in um}
     */
    public int y() {
        return y;
    }

    /**
     * {@return electrode identify}
     */
    public Object electrode() {
        return electrode;
    }

    /**
     * {@return channel identify}
     */
    public Object channel() {
        return channel;
    }

    /**
     * {@return electrode selecting state}
     */
    public int state() {
        return state;
    }

    /**
     * change the state of the electrode.
     *
     * @param state new state code
     */
    public void state(int state) {
        this.state = state;
    }

    /**
     * {@return electrode selecting category.}
     */
    public int category() {
        return category;
    }

    /**
     * change the category of the electrode
     *
     * @param category new category code.
     */
    public void category(int category) {
        this.category = category;
    }

}
