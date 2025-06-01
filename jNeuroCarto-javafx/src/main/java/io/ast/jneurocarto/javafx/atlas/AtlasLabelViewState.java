package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("AtlasBrainLabels")
public class AtlasLabelViewState {

    @JsonProperty(defaultValue = "[]")
    public List<Label> labels = new ArrayList<>();

    public static class Label {
        @JsonProperty(index = 0)
        public String text = "";

        @JsonProperty(index = 1)
        public double[] position = null;

        @JsonProperty(index = 2)
        public String color = "black";

        @JsonProperty(index = 3)
        public String type = "";

        @JsonProperty(index = 4, defaultValue = "")
        public String reference;
    }
}
