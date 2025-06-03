package io.ast.jneurocarto.javafx.atlas;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("AtlasBrainView")
public class AtlasBrainGlobalViewState {
    @JsonProperty(index = 0)
    public String default_use;

    @JsonProperty(index = 1)
    public Map<String, String> use_reference;
}
