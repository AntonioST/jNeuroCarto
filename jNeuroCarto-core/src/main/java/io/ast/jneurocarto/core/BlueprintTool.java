package io.ast.jneurocarto.core;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class BlueprintTool<T> {

    private final Blueprint<T> blueprint;

    public BlueprintTool(Blueprint<T> blueprint) {
        this.blueprint = blueprint;
    }

    public int length() {
        return blueprint.shank.length;
    }

    public int[] shank() {
        return blueprint.shank;
    }

    public int[] posx() {
        return blueprint.posx;
    }

    public int[] posy() {
        return blueprint.posy;
    }

    public int dx() {
        return blueprint.dx;
    }

    public int dy() {
        return blueprint.dy;
    }

    public int[] blueprint() {
        return blueprint.blueprint;
    }

    public int[] newBleurptint() {
        var ret = new int[length()];
        Arrays.fill(ret, ProbeDescription.CATE_UNSET);
        return ret;
    }

    public void setBlueprint(int[] blueprint) {
        var dst = this.blueprint.blueprint;
        System.arraycopy(blueprint, 0, dst, 0, dst.length);
        markDirty();
    }

    public int index(int s, int x, int y) {
        var shank = shank();
        var posx = posx();
        var posy = posy();
        for (int i = 0, length = length(); i < length; i++) {
            // posy has move unique value in general, so we test first for shortcut fail earlier.
            if (posy[i] == y && shank[i] == s && posx[i] == x) return i;
        }
        return -1;
    }

    public void markDirty() {
        blueprint.modified = true;
    }
}
