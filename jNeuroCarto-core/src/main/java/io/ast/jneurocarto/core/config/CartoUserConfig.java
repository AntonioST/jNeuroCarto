package io.ast.jneurocarto.core.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "CartoApp")
public class CartoUserConfig {

    public String theme;

    public List<String> views;

    public boolean history;

    @JsonProperty("overwrite_chmap_file")
    public boolean overwriteChmapFile;

    @JsonProperty("always_save_blueprint_file")
    public boolean alwaysSaveBlueprintFile;

    @JsonProperty("selected_as_pre_selected")
    public boolean selectedAsPreSelected;

}
