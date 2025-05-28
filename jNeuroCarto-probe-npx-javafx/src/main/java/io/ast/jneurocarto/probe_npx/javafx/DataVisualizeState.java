package io.ast.jneurocarto.probe_npx.javafx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("DataVisualizeView")
public class DataVisualizeState {

    @JsonProperty(value = "file_path", index = 0)
    public String filePath;

    @JsonProperty(value = "colormap", index = 1)
    public String colormap;
}
