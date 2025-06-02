package io.ast.jneurocarto.javafx.atlas;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("ImplantView")
public class ImplantState {
    @JsonProperty(index = 0)
    @JsonAlias({"x"})
    public double ap;

    @JsonProperty(index = 1, defaultValue = "0")
    @JsonAlias({"y"})
    public double dv;

    @JsonProperty(index = 2)
    @JsonAlias({"z"})
    public double ml;

    @JsonProperty(index = 3, defaultValue = "0")
    @JsonAlias({"s"})
    public int shank;

    @JsonProperty(index = 4, defaultValue = "0")
    @JsonAlias({"rx"})
    public double rap;

    @JsonProperty(index = 5, defaultValue = "0")
    @JsonAlias({"ry"})
    public double rdv;

    @JsonProperty(index = 6, defaultValue = "0")
    @JsonAlias({"rz"})
    public double rml;

    @JsonProperty(index = 7)
    public double depth;

    @JsonProperty(index = 8)
    public String reference = null;
}
