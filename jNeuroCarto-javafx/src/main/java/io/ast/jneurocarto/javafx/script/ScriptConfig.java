package io.ast.jneurocarto.javafx.script;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("BlueprintScriptView")
public class ScriptConfig {
    @JsonProperty(index = 0)
    public boolean clear = false;

    @JsonProperty(index = 1)
    public Map<String, String> actions = Map.of();
}
