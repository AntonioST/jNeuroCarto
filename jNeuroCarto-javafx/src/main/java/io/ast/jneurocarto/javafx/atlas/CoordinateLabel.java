package io.ast.jneurocarto.javafx.atlas;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CoordinateLabel(
  @JsonProperty(index = 0) String text,
  @JsonProperty(value = "pos", index = 1) double[] position,
  @JsonProperty(value = "ref", index = 2) int reference,
  @JsonProperty(index = 3) String color
) {

    public CoordinateLabel(String text, double x, double y, int reference, String color) {
        this(text, new double[]{x, y}, reference, color);
    }

}
