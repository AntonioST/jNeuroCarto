package io.ast.jneurocarto.core.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "CartoApp")
public class CartoUserConfig {

    public String theme;

    public List<String> views;

    public boolean history;

    @JsonAlias("overwrite_chmap_file")
    public boolean overwriteChmapFile;

    @JsonAlias("always_save_blueprint_file")
    public boolean alwaysSaveBlueprintFile;

    @JsonAlias("selected_as_pre_selected")
    public boolean selectedAsPreSelected;

}
