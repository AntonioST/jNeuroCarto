package io.ast.jneurocarto.javafx.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("Channelmap")
public class ChannelmapState {
    @JsonProperty(index = 0)
    public String family;

    @JsonProperty(index = 1)
    public String code;

    @JsonProperty(value = "channelmap_file", index = 2)
    public String channelmapFile;

    @JsonProperty(value = "blueprint_file", index = 3)
    public String blueprintFile;
}
