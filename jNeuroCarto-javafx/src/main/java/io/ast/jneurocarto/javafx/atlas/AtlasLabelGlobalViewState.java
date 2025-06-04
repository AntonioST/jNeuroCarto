package io.ast.jneurocarto.javafx.atlas;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("AtlasBrainLabels")
public class AtlasLabelGlobalViewState {

    @JsonProperty(value = "font_size", defaultValue = "20")
    public int fontSize = 20;
}
