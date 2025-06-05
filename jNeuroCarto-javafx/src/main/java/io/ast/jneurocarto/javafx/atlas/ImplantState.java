package io.ast.jneurocarto.javafx.atlas;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("ImplantView")
public class ImplantState {
    /**
     * ap insertion position in mm
     */
    @JsonProperty(index = 0)
    @JsonAlias({"x"})
    public double ap;

    /**
     * dv insertion position in mm
     */
    @JsonProperty(index = 1, defaultValue = "0")
    @JsonAlias({"y"})
    public double dv;

    /**
     * ml insertion position in mm
     */
    @JsonProperty(index = 2)
    @JsonAlias({"z"})
    public double ml;

    /**
     * the shank of the insertion position.
     */
    @JsonProperty(index = 3, defaultValue = "0")
    @JsonAlias({"s"})
    public int shank;

    /**
     * rotation along ap-axis in degree
     */
    @JsonProperty(index = 4, defaultValue = "0")
    @JsonAlias({"rx"})
    public double rap;

    /**
     * rotation along dv-axis in degree
     */
    @JsonProperty(index = 5, defaultValue = "0")
    @JsonAlias({"ry"})
    public double rdv;

    /**
     * rotation along ml-axis in degree
     */
    @JsonProperty(index = 6, defaultValue = "0")
    @JsonAlias({"rz"})
    public double rml;

    /**
     * insert depth in mm
     */
    @JsonProperty(index = 7)
    public double depth;

    /**
     * insertion position reference.
     */
    @JsonProperty(index = 8)
    public String reference = null;
}
