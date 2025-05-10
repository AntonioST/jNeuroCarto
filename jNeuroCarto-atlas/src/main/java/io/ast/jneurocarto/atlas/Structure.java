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
