package io.ast.jneurocarto.javafx.atlas;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.ast.jneurocarto.atlas.ImageSliceStack;

public class AtlasBrainViewState {
    @JsonProperty(value = "atlas_brain", index = 0, required = true)
    public String name;

    @JsonProperty(value = "brain_slice", index = 1, defaultValue = "coronal")
    public ImageSliceStack.Projection projection = ImageSliceStack.Projection.coronal;

    @JsonProperty(value = "slice_plane", index = 2, defaultValue = "0")
    public int plane;

    @JsonProperty(value = "slice_rot_w", index = 3, defaultValue = "0")
    public int offsetWidth;

    @JsonProperty(value = "slice_rot_h", index = 4, defaultValue = "0")
    public int offsetHeight;

    @JsonProperty(value = "image_dx", index = 5, defaultValue = "0")
    public double imagePosX;

    @JsonProperty(value = "image_dy", index = 6, defaultValue = "0")
    public double imagePosY;

    @JsonProperty(value = "image_sx", index = 7, defaultValue = "0")
    public double imageScaleX;

    @JsonProperty(value = "image_sy", index = 8, defaultValue = "0")
    public double imageScaleY;

    @JsonProperty(value = "image_rt", index = 9, defaultValue = "0")
    public double imageRoration;

    @JsonProperty(value = "image_alpha", index = 10, defaultValue = "1")
    public double imageAlpha;

    @JsonProperty(index = 11, defaultValue = "[]")
    public List<String> regions = new ArrayList<>();

    @JsonProperty(index = 12, defaultValue = "[]")
    public List<CoordinateLabel> labels = new ArrayList<>();
}
