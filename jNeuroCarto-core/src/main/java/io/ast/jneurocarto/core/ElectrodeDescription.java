package io.ast.jneurocarto.core;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/**
 * An electrode interface for GUI interaction between different electrode implementations.
 *
 * @param s         shank
 * @param x         x position in um
 * @param y         y position in um
 * @param electrode electrode identify. It should be a hashable.
 * @param channel   channel identify. It is used for display (str-able).
 * @param state     electrode selecting state
 * @param category  electrode selecting category.
 */
@NullMarked
public record ElectrodeDescription(
  int s,
  int x,
  int y,
  Object electrode,
  Object channel,
  int state,
  int category
) {

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
}
