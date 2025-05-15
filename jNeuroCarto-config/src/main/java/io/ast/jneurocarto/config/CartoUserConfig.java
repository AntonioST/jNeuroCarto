package io.ast.jneurocarto.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "CartoApp")
public class CartoUserConfig {

    public String theme;
    public List<String> views;
    public boolean history;
    public boolean overwriteChmapFile;
    public boolean alwaysSaveBlueprintFile;
    public boolean selectedAsPreSelected;

}
