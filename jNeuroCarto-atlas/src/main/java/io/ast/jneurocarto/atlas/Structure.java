package io.ast.jneurocarto.atlas;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Structure(
  String acronym,
  int id,
  String name,
  @JsonAlias("structure_id_path") int[] structurePath,
  @JsonAlias("rgb_triplet") int[] rgbTriplet
) {

    public boolean isRoot() {
        return structurePath.length == 1;
    }

    public int parent() {
        if (structurePath.length == 1) {
            return structurePath[0];
        }
        return structurePath[structurePath.length - 2];
    }

    public boolean hasParent(int id) {
        for (int j : structurePath) {
            if (j == id) return true;
        }
        return false;
    }

    public boolean hasParent(Structure parent) {
        return hasParent(parent.id);
    }

    @Override
    public String toString() {
        return "Structure{" +
               "acronym='" + acronym + '\'' +
               ", id=" + id +
               ", name='" + name + '\'' +
               ", structurePath=" + Arrays.toString(structurePath) +
               ", rgbTriplet=" + Arrays.toString(rgbTriplet) +
               '}';
    }
}
