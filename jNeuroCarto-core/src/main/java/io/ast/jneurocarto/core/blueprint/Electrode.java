package io.ast.jneurocarto.core.blueprint;

/**
 * An electrode data that used in {@link Blueprint} for filtering.
 *
 * @param i electrode index.
 * @param s shank number
 * @param x x position
 * @param y y position
 * @param c category
 */
public record Electrode(int i, int s, int x, int y, int c) {
}
