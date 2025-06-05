package io.ast.jneurocarto.atlas;

public record ImageArray(int w, int h, int[] image) {

    public int size() {
        return w * h;
    }

    public boolean inBoundary(int x, int y) {
        return 0 <= x && x < w && 0 <= y && y < h;
    }

    public int index(int x, int y) {
        if (!inBoundary(x, y)) throw new IllegalArgumentException();
        return y * w + x;
    }

    public int get(int x, int y) {
        return image[index(x, y)];
    }
}
