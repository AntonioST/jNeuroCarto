package io.ast.jneurocarto.javafx.atlas;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

public class CoordinateLabel {

    public String text;

    @JsonAlias("pos")
    public List<Double> position;

    @JsonAlias("ref")
    public int reference;

    public String color;
}
